package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The four axe passives that act on what a chopped block drops. They share one
 * end-of-tick pass because they compose in a fixed order and must never touch
 * the same drop twice:
 *
 *   Double Axe I/II  — logs drop double, 10% / 20% per log (axe only)
 *   Pruner           — leaves broken with an axe drop double loot
 *   Fair Harvest     — +25% sapling chance from any leaf you break
 *   Logger's Magnet  — whatever is left goes straight into the inventory
 *
 * Drops are collected at the END of the tick, once every drop entity has
 * actually spawned — scanning inside the break event misses late spawns. Same
 * trick SmeltHandler uses, and it is what makes Double Axe compose with a Logic
 * fell: every felled log fires its own break event, so every log rolls on its
 * own. TimberScheduler fells synchronously inside the break event, so by
 * the time {@link #tick} runs at the end of the tick every drop is already on
 * the ground.
 */
public final class AxeHarvest {
	private static final double DROP_RADIUS = 1.5;
	private static final int FAIR_HARVEST_PERCENT = 25;

	/** Which sapling a leaf block may yield (Fair Harvest). */
	private static final Map<Block, Block> LEAF_TO_SAPLING = Map.ofEntries(
		Map.entry(Blocks.OAK_LEAVES, Blocks.OAK_SAPLING),
		Map.entry(Blocks.SPRUCE_LEAVES, Blocks.SPRUCE_SAPLING),
		Map.entry(Blocks.BIRCH_LEAVES, Blocks.BIRCH_SAPLING),
		Map.entry(Blocks.JUNGLE_LEAVES, Blocks.JUNGLE_SAPLING),
		Map.entry(Blocks.ACACIA_LEAVES, Blocks.ACACIA_SAPLING),
		Map.entry(Blocks.DARK_OAK_LEAVES, Blocks.DARK_OAK_SAPLING),
		Map.entry(Blocks.MANGROVE_LEAVES, Blocks.MANGROVE_PROPAGULE),
		Map.entry(Blocks.CHERRY_LEAVES, Blocks.CHERRY_SAPLING),
		Map.entry(Blocks.PALE_OAK_LEAVES, Blocks.PALE_OAK_SAPLING),
		Map.entry(Blocks.AZALEA_LEAVES, Blocks.AZALEA),
		Map.entry(Blocks.FLOWERING_AZALEA_LEAVES, Blocks.FLOWERING_AZALEA)
	);

	private record Pending(ServerLevel level, UUID playerId, BlockPos pos,
	                       int doubleAxePercent, boolean pruner, boolean magnet,
	                       @Nullable Block fairHarvestSapling) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private AxeHarvest() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		boolean axe = serverPlayer.getMainHandItem().is(ItemTags.AXES);
		boolean leaf = state.is(BlockTags.LEAVES);

		int doubleAxePercent = 0;
		if (axe && state.is(BlockTags.LOGS)) {
			doubleAxePercent = switch (PerkAccess.rank(serverPlayer, SkillTrees.AXE, "double_axe_1", "double_axe_2")) {
				case 1 -> 10;
				case 2 -> 20;
				default -> 0;
			};
		}
		boolean pruner = axe && leaf && SkillService.owns(serverPlayer, SkillTrees.AXE, "pruner");
		boolean magnet = axe && SkillService.owns(serverPlayer, SkillTrees.AXE, "loggers_magnet");
		// Fair Harvest reads as a property of the leaves, not of the tool.
		Block sapling = leaf && SkillService.owns(serverPlayer, SkillTrees.AXE, "fair_harvest")
			? LEAF_TO_SAPLING.get(state.getBlock())
			: null;

		if (doubleAxePercent == 0 && !pruner && !magnet && sapling == null) {
			return;
		}
		PENDING.add(new Pending(serverLevel, serverPlayer.getUUID(), pos, doubleAxePercent, pruner, magnet, sapling));
	}

	/** Called at the end of every server tick, once every drop of the tick has spawned. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		// One drop entity belongs to exactly one break: adjacent logs felled in
		// the same tick have overlapping search boxes, and doubling a drop twice
		// would quadruple it.
		Set<Integer> claimed = new HashSet<>();

		for (Pending pending : PENDING) {
			RandomSource random = pending.level().getRandom();
			List<ItemEntity> drops = new ArrayList<>();
			for (ItemEntity candidate : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(DROP_RADIUS), entity -> entity.tickCount <= 1)) {
				if (claimed.add(candidate.getId())) {
					drops.add(candidate);
				}
			}

			List<ItemEntity> bonus = new ArrayList<>();
			for (ItemEntity drop : drops) {
				ItemStack stack = drop.getItem();
				int extra = 0;
				if (pending.doubleAxePercent() > 0 && stack.is(ItemTags.LOGS)) {
					extra += roll(random, stack.getCount(), pending.doubleAxePercent());
				}
				if (pending.pruner()) {
					extra += stack.getCount();
				}
				if (extra > 0) {
					bonus.add(spawn(pending.level(), drop.blockPosition(), stack.copyWithCount(extra)));
				}
			}
			if (pending.fairHarvestSapling() != null && random.nextInt(100) < FAIR_HARVEST_PERCENT) {
				bonus.add(spawn(pending.level(), pending.pos(),
					new ItemStack(pending.fairHarvestSapling().asItem())));
			}
			// A bonus drop is fresh (tickCount 0), so the next pending break in
			// this same pass would otherwise find it and double it again.
			for (ItemEntity extraDrop : bonus) {
				claimed.add(extraDrop.getId());
			}
			drops.addAll(bonus);

			if (pending.magnet()) {
				ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
				if (player != null) {
					for (ItemEntity drop : drops) {
						if (!drop.isRemoved()) {
							drop.setNoPickUpDelay();
							drop.playerTouch(player); // handles partial pickup, sound and the pickup animation
						}
					}
				}
			}
		}
		PENDING.clear();
	}

	/** Rolls {@code percent} once per item so partial stacks double partially. */
	private static int roll(RandomSource random, int count, int percent) {
		int extra = 0;
		for (int i = 0; i < count; i++) {
			if (random.nextInt(100) < percent) {
				extra++;
			}
		}
		return extra;
	}

	private static ItemEntity spawn(ServerLevel level, BlockPos pos, ItemStack stack) {
		ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		entity.setDefaultPickUpDelay();
		level.addFreshEntity(entity);
		return entity;
	}
}
