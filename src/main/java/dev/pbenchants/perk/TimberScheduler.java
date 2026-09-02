package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.track.BlockBreakTracker;
import dev.pbenchants.track.PlacedLogs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logic (timber) — driven by the Logic enchantment on the axe, inspired by
 * FallingTree/Treecapitator, with the one-tree-at-a-time detection the
 * tree-feller family of plugins converged on:
 *
 * - Only fells actual trees: the connected logs must touch enough leaves,
 *   and a log a player placed is never part of a tree — {@link PlacedLogs}
 *   remembers placements, so log houses and pillars stay standing even when
 *   a grown tree leans against them.
 * - One swing fells ONE tree. Fused canopies (cherry groves, jungle tangles)
 *   used to come down as a single connected component; now each rooted trunk
 *   base claims the logs nearest to it, and only the origin's tree falls.
 *   A 2x2 giant (jungle, spruce, dark oak) is still one tree — its four
 *   bases touch, so they cluster into one trunk.
 * - Level 1 pays with a slower chop on the initial log (see PlayerMixin);
 *   levels 2+ chop at normal speed. Level 3 also clears the canopy — but only
 *   the leaves this tree owns: a leaf is broken only when the felled logs are
 *   its nearest logs (vanilla's own leaf DISTANCE says so), so a neighbouring
 *   tree keeps its crown.
 * - Every log costs durability, and the fell finishes the tree: if the wood
 *   outlasts the axe, the axe breaks mid-fell and the rest comes down by
 *   hand. Indestructible stops the loss the way it always does — the axe
 *   survives on its last durability point, spent.
 * - Sneaking disables.
 */
public final class TimberScheduler {
	private static final int MAX_LOGS = 256;
	private static final int MAX_LEAVES = 512;
	private static final int MIN_LEAVES_FOR_TREE = 4;
	/** Vanilla leaves track their distance to the nearest log up to 7. */
	private static final int MAX_LEAF_DISTANCE = 7;

	/** Which sapling regrows each log type (Environment enchantment). */
	private static final java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> LOG_TO_SAPLING = java.util.Map.ofEntries(
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.OAK_LOG, net.minecraft.world.level.block.Blocks.OAK_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.SPRUCE_LOG, net.minecraft.world.level.block.Blocks.SPRUCE_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.BIRCH_LOG, net.minecraft.world.level.block.Blocks.BIRCH_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.JUNGLE_LOG, net.minecraft.world.level.block.Blocks.JUNGLE_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.ACACIA_LOG, net.minecraft.world.level.block.Blocks.ACACIA_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.DARK_OAK_LOG, net.minecraft.world.level.block.Blocks.DARK_OAK_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.MANGROVE_LOG, net.minecraft.world.level.block.Blocks.MANGROVE_PROPAGULE),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.CHERRY_LOG, net.minecraft.world.level.block.Blocks.CHERRY_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.PALE_OAK_LOG, net.minecraft.world.level.block.Blocks.PALE_OAK_SAPLING),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.CRIMSON_STEM, net.minecraft.world.level.block.Blocks.CRIMSON_FUNGUS),
		java.util.Map.entry(net.minecraft.world.level.block.Blocks.WARPED_STEM, net.minecraft.world.level.block.Blocks.WARPED_FUNGUS)
	);

	/**
	 * True while this thread is inside a Logic fell. The fell may consume the
	 * axe partway — that is the new deal — but every log of the fell was still
	 * paid for by an axe swing, so the drop passives (Double Axe, Logger's
	 * Magnet) must keep treating the nested breaks as axe chops even once the
	 * hand is empty. {@link BreakGuard#busy()} is not enough: it is also true
	 * inside shovel and pickaxe cascades, which are not axe swings.
	 */
	private static final ThreadLocal<Integer> FELL_DEPTH = ThreadLocal.withInitial(() -> 0);

	private TimberScheduler() {
	}

	/** Is this break part of a Logic fell — an axe chop whatever the hand holds now? */
	public static boolean felling() {
		return FELL_DEPTH.get() > 0;
	}

	/** @deprecated superseded by {@link BreakGuard#busy()}; kept for older callers. */
	@Deprecated
	public static boolean isTimberBreaking() {
		return BreakGuard.busy();
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BreakGuard.busy()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown() || !state.is(BlockTags.LOGS)) {
			return;
		}
		ItemStack axe = serverPlayer.getMainHandItem();
		if (!axe.is(ItemTags.AXES)) {
			return;
		}
		int logicLevel = ItemAuthority.effectiveLevel(serverPlayer, axe, ModEnchantments.LOGIC);
		if (logicLevel <= 0) {
			return;
		}
		if (PlacedLogs.isPlaced(serverLevel, pos)) {
			return; // a hand-placed log is a wall, not a tree
		}

		Scan scan = scanTree(serverLevel, pos);
		if (scan.logs().isEmpty() || scan.leafCount() < MIN_LEAVES_FOR_TREE) {
			return; // not a tree (log house, fence post, floating stump...)
		}

		// Read off the axe before the fell — the fell may consume it, and an
		// empty stack answers zero to every enchantment.
		boolean environment = logicLevel >= 3
			&& ItemAuthority.effectiveLevel(serverPlayer, axe, ModEnchantments.ENVIRONMENT) > 0;

		boolean breakLeaves = logicLevel >= 3;
		fell(serverPlayer, serverLevel, scan, breakLeaves);

		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_with_logic", 1);
		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_trees_total", 1);
		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_trees_grand_total", 1);

		if (environment) {
			// Replant on the tree's own rooted bases — wherever on the trunk the
			// chop landed. Only when no base was found (a tree the ground scan
			// could not root) does the broken block itself stand in.
			replant(serverPlayer, serverLevel,
				scan.stumps().isEmpty() ? List.of(pos) : scan.stumps(), state);
		}
	}

	private record Scan(List<BlockPos> logs, List<BlockPos> leaves, int leafCount, List<BlockPos> stumps) {
	}

	/**
	 * Finds the one tree the broken log belongs to.
	 *
	 * <p>First the whole connected component of grown logs is flood-filled
	 * (26-neighbourhood) — hand-placed logs are not logs as far as Logic is
	 * concerned, so the fill never crosses into a build. The frontier is always
	 * drained completely: even when the log cap cuts the fill short, every
	 * accepted log still gets its neighbours checked for leaves, so the "is
	 * this actually a tree?" test does not fail just because the cap landed
	 * below the canopy.
	 *
	 * <p>Then the component is split into trees. A log standing on ground
	 * (dirt, nylium, mangrove roots) is a trunk base; touching bases cluster
	 * into one trunk, which keeps a 2x2 giant whole. With more than one trunk
	 * in the component — fused cherry or jungle neighbours — a breadth-first
	 * wave grows out from every trunk at once and each log is claimed by the
	 * nearest one. Only the origin's tree is felled.
	 *
	 * <p>Last, the felled tree's own canopy: walking outward from the felled
	 * logs through orthogonal leaf steps, a leaf reached in {@code d} steps is
	 * ours only when its vanilla DISTANCE property is exactly {@code d} — any
	 * smaller and some standing log is closer, so the leaf (and everything
	 * behind it) belongs to a tree that is not coming down.
	 */
	private static Scan scanTree(ServerLevel level, BlockPos origin) {
		// --- The connected component of grown logs, plus its leaf shell ---
		Set<BlockPos> component = new HashSet<>();
		List<BlockPos> componentOrder = new ArrayList<>();
		Set<BlockPos> shellLeaves = new HashSet<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		component.add(origin);
		frontier.add(origin);

		while (!frontier.isEmpty()) {
			BlockPos current = frontier.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.offset(dx, dy, dz);
						if (component.contains(next)) {
							continue;
						}
						BlockState nextState = level.getBlockState(next);
						if (nextState.is(BlockTags.LOGS)) {
							if (componentOrder.size() < MAX_LOGS && !PlacedLogs.isPlaced(level, next)) {
								component.add(next);
								componentOrder.add(next);
								frontier.add(next);
							}
						} else if (isNaturalLeaf(nextState)) {
							shellLeaves.add(next);
						}
					}
				}
			}
		}

		// --- Trunk bases: logs standing on ground, clustered by touch ---
		List<BlockPos> roots = new ArrayList<>();
		for (BlockPos log : component) {
			BlockPos below = log.below();
			if (!component.contains(below) && isTreeGround(level.getBlockState(below))) {
				roots.add(log);
			}
		}
		Map<BlockPos, Integer> owner = new HashMap<>();
		int clusters = 0;
		for (BlockPos root : roots) {
			if (owner.containsKey(root)) {
				continue;
			}
			int id = clusters++;
			ArrayDeque<BlockPos> cluster = new ArrayDeque<>();
			owner.put(root, id);
			cluster.add(root);
			while (!cluster.isEmpty()) {
				BlockPos current = cluster.poll();
				for (BlockPos other : roots) {
					if (!owner.containsKey(other) && touching(current, other)) {
						owner.put(other, id);
						cluster.add(other);
					}
				}
			}
		}

		// --- One tree: every log claims the nearest trunk, origin's wins ---
		List<BlockPos> felled = componentOrder;
		Integer originTree = null;
		if (clusters > 1) {
			ArrayDeque<BlockPos> wave = new ArrayDeque<>(owner.keySet());
			while (!wave.isEmpty()) {
				BlockPos current = wave.poll();
				Integer id = owner.get(current);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							BlockPos next = current.offset(dx, dy, dz);
							if (component.contains(next) && !owner.containsKey(next)) {
								owner.put(next, id);
								wave.add(next);
							}
						}
					}
				}
			}
			originTree = owner.get(origin);
			if (originTree != null) {
				felled = new ArrayList<>();
				for (BlockPos log : componentOrder) {
					if (originTree.equals(owner.get(log))) {
						felled.add(log);
					}
				}
			}
		}

		// --- The felled tree's own rooted bases: where Environment replants ---
		List<BlockPos> stumps = new ArrayList<>();
		for (BlockPos root : roots) {
			if (originTree == null || originTree.equals(owner.get(root))) {
				stumps.add(root);
			}
		}

		// --- This tree's own canopy, judged by vanilla's leaf DISTANCE ---
		List<BlockPos> canopy = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>(felled);
		visited.add(origin);
		List<BlockPos> layer = new ArrayList<>(felled);
		layer.add(origin);
		for (int depth = 1; depth <= MAX_LEAF_DISTANCE && !layer.isEmpty() && canopy.size() < MAX_LEAVES; depth++) {
			List<BlockPos> nextLayer = new ArrayList<>();
			for (BlockPos from : layer) {
				for (Direction direction : Direction.values()) {
					BlockPos next = from.relative(direction);
					if (!visited.add(next) || canopy.size() >= MAX_LEAVES) {
						continue;
					}
					BlockState nextState = level.getBlockState(next);
					if (!isNaturalLeaf(nextState) || leafDistance(nextState) != depth) {
						continue; // not ours: some log still standing is closer
					}
					canopy.add(next);
					nextLayer.add(next);
				}
			}
			layer = nextLayer;
		}
		return new Scan(felled, canopy, shellLeaves.size(), stumps);
	}

	/** Natural (world-generated) leaves only — player-placed hedges are persistent. */
	private static boolean isNaturalLeaf(BlockState state) {
		return state.is(BlockTags.LEAVES)
			&& (!state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT));
	}

	/** Vanilla's own distance-to-nearest-log, 1..7 on every leaf. */
	private static int leafDistance(BlockState state) {
		return state.hasProperty(LeavesBlock.DISTANCE) ? state.getValue(LeavesBlock.DISTANCE) : Integer.MAX_VALUE;
	}

	/** What a tree trunk stands on: dirt family, nylium (huge fungi), mangrove roots. */
	private static boolean isTreeGround(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.NYLIUM) || state.is(Blocks.MANGROVE_ROOTS);
	}

	/** Chebyshev adjacency — diagonal counts, so a 2x2 base is one cluster. */
	private static boolean touching(BlockPos a, BlockPos b) {
		return Math.abs(a.getX() - b.getX()) <= 1
			&& Math.abs(a.getY() - b.getY()) <= 1
			&& Math.abs(a.getZ() - b.getZ()) <= 1;
	}

	/**
	 * Breaks the whole scan at once. Logs cost durability and the fell does not
	 * stop for a dying axe — the tree comes down whole, the axe with it if the
	 * wood outlasts it (Indestructible clamps that loss to "spent", on the
	 * durability path it already guards). Leaves cost nothing.
	 */
	private static void fell(ServerPlayer player, ServerLevel level, Scan scan, boolean breakLeaves) {
		BreakGuard.enter();
		FELL_DEPTH.set(FELL_DEPTH.get() + 1);
		try {
			for (BlockPos next : scan.logs()) {
				if (level.getBlockState(next).is(BlockTags.LOGS)) {
					player.gameMode.destroyBlock(next);
				}
			}
			if (breakLeaves) {
				for (BlockPos next : scan.leaves()) {
					if (level.getBlockState(next).is(BlockTags.LEAVES)) {
						level.destroyBlock(next, true, player, 512);
						// destroyBlock raises no break event, so the canopy this
						// perk clears has to be counted by hand or Timber III
						// would stall the very gate it is helping fill.
						BlockBreakTracker.countLeaf(player);
					}
				}
			}
		} finally {
			FELL_DEPTH.set(FELL_DEPTH.get() - 1);
			BreakGuard.exit();
		}
	}

	/**
	 * Environment: puts a sapling back on every rooted base of the felled tree,
	 * consuming one from the player per sapling planted — the same base-first
	 * replant the Tree Harvester family of mods does, so it works wherever on
	 * the trunk the chop landed. A 2x2 giant (jungle, spruce, dark oak) has
	 * four bases and gets all four saplings back, which is exactly what it
	 * takes to regrow one; run out of saplings and the remaining bases stay
	 * bare.
	 */
	private static void replant(ServerPlayer player, ServerLevel level, List<BlockPos> stumps, BlockState brokenState) {
		net.minecraft.world.level.block.Block sapling = LOG_TO_SAPLING.get(brokenState.getBlock());
		if (sapling == null) {
			return;
		}
		BlockState saplingState = sapling.defaultBlockState();
		ItemStack saplingItem = new ItemStack(sapling.asItem());
		for (BlockPos stump : stumps) {
			// Farmland-style surprises aside, the base can be occupied (a felled
			// neighbour's drop entity is fine, a block is not) or its ground gone.
			if (!level.getBlockState(stump).isAir() || !saplingState.canSurvive(level, stump)) {
				continue;
			}
			if (!takeSapling(player, saplingItem)) {
				return; // out of saplings — the remaining bases stay bare
			}
			level.setBlockAndUpdate(stump, saplingState);
			SkillService.addCount(player, SkillTrees.AXE, "replant_with_environment", 1);
		}
	}

	/** One sapling out of the player's inventory; false when they carry none. */
	private static boolean takeSapling(ServerPlayer player, ItemStack saplingItem) {
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inSlot = inventory.getItem(slot);
			if (ItemStack.isSameItem(inSlot, saplingItem)) {
				inSlot.shrink(1);
				return true;
			}
		}
		return false;
	}
}
