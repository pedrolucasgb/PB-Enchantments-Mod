package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Logic (timber) — driven by the Logic enchantment on the axe, inspired by
 * FallingTree/Treecapitator:
 *
 * - Only fells actual trees: the connected logs must touch enough leaves,
 *   so log-built houses stay standing.
 * - Every level fells all connected logs at once. Level 1 pays for it with a
 *   slower chop on the initial log (see PlayerMixin); levels 2+ chop at
 *   normal speed. Level 3 also clears the canopy's natural leaves.
 * - Stops before the axe would break; each log costs durability as usual.
 *   Leaves cost no durability.
 * - Sneaking disables.
 */
public final class TimberScheduler {
	private static final int MAX_LOGS = 256;
	private static final int MAX_LEAVES = 512;
	private static final int MIN_LEAVES_FOR_TREE = 4;
	/** How far leaves spread outward from the logs before we stop collecting. */
	private static final int LEAF_SPREAD = 5;

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

	private static boolean breakingNow = false;

	private TimberScheduler() {
	}

	public static boolean isTimberBreaking() {
		return breakingNow;
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (breakingNow || AreaBreak.isAreaBreaking()) {
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
		int logicLevel = ModEnchantments.level(serverPlayer, axe, ModEnchantments.LOGIC);
		if (logicLevel <= 0) {
			return;
		}

		Scan scan = scanTree(serverLevel, pos);
		if (scan.logs().isEmpty() || scan.leafCount() < MIN_LEAVES_FOR_TREE) {
			return; // not a tree (log house, fence post, floating stump...)
		}

		boolean breakLeaves = logicLevel >= 3;
		fell(serverPlayer, serverLevel, scan, breakLeaves);

		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_with_logic", 1);
		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_trees_total", 1);
		SkillService.addCount(serverPlayer, SkillTrees.AXE, "fell_trees_grand_total", 1);

		if (logicLevel >= 3 && ModEnchantments.level(serverPlayer, axe, ModEnchantments.ENVIRONMENT) > 0) {
			replant(serverPlayer, serverLevel, pos, state);
		}
	}

	private record Scan(ArrayDeque<BlockPos> logs, ArrayDeque<BlockPos> leaves, int leafCount) {
	}

	/**
	 * Flood-fills connected logs (26-neighborhood) from the broken block, then
	 * grows outward from the log-adjacent leaves to cover the whole canopy.
	 *
	 * The frontier is always drained completely: even when the log cap cuts
	 * the fell short (2x2 giants, merged forests), every accepted log still
	 * gets its neighbors checked for leaves, so the "is this actually a
	 * tree?" test no longer fails just because the cap landed below the
	 * canopy.
	 */
	private static Scan scanTree(ServerLevel level, BlockPos origin) {
		ArrayDeque<BlockPos> logs = new ArrayDeque<>();
		Set<BlockPos> visitedLogs = new HashSet<>();
		Set<BlockPos> visitedLeaves = new HashSet<>();
		ArrayDeque<BlockPos> leaves = new ArrayDeque<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(origin);
		visitedLogs.add(origin);

		while (!frontier.isEmpty()) {
			BlockPos current = frontier.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.offset(dx, dy, dz);
						BlockState nextState = level.getBlockState(next);
						if (nextState.is(BlockTags.LOGS)) {
							if (logs.size() < MAX_LOGS && visitedLogs.add(next)) {
								logs.add(next);
								frontier.add(next);
							}
						} else if (isNaturalLeaf(nextState)) {
							if (visitedLeaves.add(next) && leaves.size() < MAX_LEAVES) {
								leaves.add(next);
							}
						}
					}
				}
			}
		}

		// The log-adjacent leaves are only the canopy's inner shell; walk
		// outward through connected leaves so level 3 clears the whole crown.
		List<BlockPos> layer = new ArrayList<>(leaves);
		for (int depth = 0; depth < LEAF_SPREAD && !layer.isEmpty() && leaves.size() < MAX_LEAVES; depth++) {
			List<BlockPos> nextLayer = new ArrayList<>();
			for (BlockPos leaf : layer) {
				for (int dx = -1; dx <= 1; dx++) {
					for (int dy = -1; dy <= 1; dy++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (dx == 0 && dy == 0 && dz == 0) {
								continue;
							}
							BlockPos next = leaf.offset(dx, dy, dz);
							if (!visitedLeaves.add(next) || leaves.size() >= MAX_LEAVES) {
								continue;
							}
							if (isNaturalLeaf(level.getBlockState(next))) {
								leaves.add(next);
								nextLayer.add(next);
							}
						}
					}
				}
			}
			layer = nextLayer;
		}
		return new Scan(logs, leaves, leaves.size());
	}

	/** Natural (world-generated) leaves only — player-placed hedges are persistent. */
	private static boolean isNaturalLeaf(BlockState state) {
		return state.is(BlockTags.LEAVES)
			&& (!state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT));
	}

	/** Breaks the whole scan at once. Logs cost durability; leaves do not. */
	private static void fell(ServerPlayer player, ServerLevel level, Scan scan, boolean breakLeaves) {
		breakingNow = true;
		try {
			while (!scan.logs().isEmpty()) {
				if (axeAboutToBreak(player)) {
					return;
				}
				BlockPos next = scan.logs().poll();
				if (level.getBlockState(next).is(BlockTags.LOGS)) {
					player.gameMode.destroyBlock(next);
				}
			}
			if (breakLeaves) {
				while (!scan.leaves().isEmpty()) {
					BlockPos next = scan.leaves().poll();
					if (level.getBlockState(next).is(BlockTags.LEAVES)) {
						level.destroyBlock(next, true, player, 512);
					}
				}
			}
		} finally {
			breakingNow = false;
		}
	}

	private static boolean axeAboutToBreak(ServerPlayer player) {
		ItemStack axe = player.getMainHandItem();
		return !axe.is(ItemTags.AXES)
			|| (axe.isDamageableItem() && axe.getDamageValue() >= axe.getMaxDamage() - 2);
	}

	/** Environment: puts a sapling back on the stump, consuming one from the player. */
	private static void replant(ServerPlayer player, ServerLevel level, BlockPos stump, BlockState brokenState) {
		net.minecraft.world.level.block.Block sapling = LOG_TO_SAPLING.get(brokenState.getBlock());
		if (sapling == null) {
			return;
		}
		BlockState saplingState = sapling.defaultBlockState();
		if (!level.getBlockState(stump).isAir() || !saplingState.canSurvive(level, stump)) {
			return;
		}
		ItemStack saplingItem = new ItemStack(sapling.asItem());
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inSlot = inventory.getItem(slot);
			if (ItemStack.isSameItem(inSlot, saplingItem)) {
				inSlot.shrink(1);
				level.setBlockAndUpdate(stump, saplingState);
				SkillService.addCount(player, SkillTrees.AXE, "replant_with_environment", 1);
				return;
			}
		}
	}
}
