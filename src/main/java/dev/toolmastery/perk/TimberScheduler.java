package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Logic (timber) — driven by the Logic enchantment on the axe, inspired by
 * FallingTree/Treecapitator:
 *
 * - Only fells actual trees: the connected logs must touch enough leaves,
 *   so log-built houses stay standing.
 * - Level 1 fells in a visible cascade (a batch of logs per tick).
 *   Level 2 fells instantly. Level 3 also clears the leaves.
 * - Stops before the axe would break; each log costs durability as usual.
 * - Sneaking disables.
 */
public final class TimberScheduler {
	private static final int MAX_LOGS = 128;
	private static final int MAX_LEAVES = 256;
	private static final int MIN_LEAVES_FOR_TREE = 4;
	private static final int SLOW_LOGS_PER_TICK = 2; // level 1: ~40 logs/second

	private record Job(UUID playerId, ServerLevel level, ArrayDeque<BlockPos> logs, ArrayDeque<BlockPos> leaves,
	                   boolean breakLeaves, BlockPos stump, @org.jetbrains.annotations.Nullable net.minecraft.world.level.block.Block sapling) {
	}

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

	private static final List<Job> JOBS = new ArrayList<>();
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
		boolean replant = logicLevel >= 3
			&& ModEnchantments.level(serverPlayer, axe, ModEnchantments.ENVIRONMENT) > 0;
		net.minecraft.world.level.block.Block sapling = replant ? LOG_TO_SAPLING.get(state.getBlock()) : null;
		Job job = new Job(serverPlayer.getUUID(), serverLevel, scan.logs(), scan.leaves(), breakLeaves, pos, sapling);

		if (logicLevel >= 2) {
			// Instant fell: drain the whole job right now.
			while (step(job, Integer.MAX_VALUE)) {
				// keep going
			}
			finish(serverPlayer, job);
		} else {
			JOBS.add(job);
		}
	}

	private record Scan(ArrayDeque<BlockPos> logs, ArrayDeque<BlockPos> leaves, int leafCount) {
	}

	/**
	 * Flood-fills connected logs (26-neighborhood) from the broken block and
	 * gathers the leaves touching them. The leaf count doubles as the
	 * "is this actually a tree?" test.
	 */
	private static Scan scanTree(ServerLevel level, BlockPos origin) {
		ArrayDeque<BlockPos> logs = new ArrayDeque<>();
		ArrayDeque<BlockPos> leaves = new ArrayDeque<>();
		Set<BlockPos> visitedLogs = new HashSet<>();
		Set<BlockPos> visitedLeaves = new HashSet<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(origin);
		visitedLogs.add(origin);

		while (!frontier.isEmpty() && logs.size() < MAX_LOGS) {
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
							if (visitedLogs.add(next)) {
								logs.add(next);
								frontier.add(next);
							}
						} else if (nextState.is(BlockTags.LEAVES)) {
							if (visitedLeaves.add(next) && leaves.size() < MAX_LEAVES) {
								leaves.add(next);
							}
						}
					}
				}
			}
		}
		return new Scan(logs, leaves, visitedLeaves.size());
	}

	/** Breaks up to {@code budget} blocks of the job. Returns true while work remains. */
	private static boolean step(Job job, int budget) {
		ServerPlayer player = job.level().getServer().getPlayerList().getPlayer(job.playerId());
		if (player == null) {
			job.logs().clear();
			job.leaves().clear();
			return false;
		}

		int broken = 0;
		breakingNow = true;
		try {
			while (broken < budget && !job.logs().isEmpty()) {
				if (axeAboutToBreak(player)) {
					job.logs().clear();
					job.leaves().clear();
					return false;
				}
				BlockPos next = job.logs().poll();
				if (job.level().getBlockState(next).is(BlockTags.LOGS)) {
					player.gameMode.destroyBlock(next);
					broken++;
				}
			}
			if (job.breakLeaves()) {
				while (broken < budget && !job.leaves().isEmpty()) {
					BlockPos next = job.leaves().poll();
					if (job.level().getBlockState(next).is(BlockTags.LEAVES)) {
						player.gameMode.destroyBlock(next);
						broken++;
					}
				}
			}
		} finally {
			breakingNow = false;
		}
		return !job.logs().isEmpty() || (job.breakLeaves() && !job.leaves().isEmpty());
	}

	private static boolean axeAboutToBreak(ServerPlayer player) {
		ItemStack axe = player.getMainHandItem();
		return !axe.is(ItemTags.AXES)
			|| (axe.isDamageableItem() && axe.getDamageValue() >= axe.getMaxDamage() - 2);
	}

	private static void finish(ServerPlayer player, Job job) {
		SkillService.addCount(player, SkillTrees.AXE, "fell_with_logic", 1);
		SkillService.addCount(player, SkillTrees.AXE, "fell_trees_total", 1);
		SkillService.addCount(player, SkillTrees.AXE, "fell_trees_grand_total", 1);
		replant(player, job);
	}

	/** Environment: puts a sapling back on the stump, consuming one from the player. */
	private static void replant(ServerPlayer player, Job job) {
		if (job.sapling() == null) {
			return;
		}
		BlockState saplingState = job.sapling().defaultBlockState();
		BlockPos stump = job.stump();
		if (!job.level().getBlockState(stump).isAir() || !saplingState.canSurvive(job.level(), stump)) {
			return;
		}
		ItemStack saplingItem = new ItemStack(job.sapling().asItem());
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inSlot = inventory.getItem(slot);
			if (ItemStack.isSameItem(inSlot, saplingItem)) {
				inSlot.shrink(1);
				job.level().setBlockAndUpdate(stump, saplingState);
				SkillService.addCount(player, SkillTrees.AXE, "replant_with_environment", 1);
				return;
			}
		}
	}

	/** Called once per server tick: advances slow (level 1) jobs. */
	public static void tick(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		Iterator<Job> iterator = JOBS.iterator();
		while (iterator.hasNext()) {
			Job job = iterator.next();
			boolean hasMore = step(job, SLOW_LOGS_PER_TICK);
			if (!hasMore) {
				ServerPlayer player = server.getPlayerList().getPlayer(job.playerId());
				if (player != null) {
					finish(player, job);
				}
				iterator.remove();
			}
		}
	}
}
