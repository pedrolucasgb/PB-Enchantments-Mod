package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Melt — chance to smelt ore drops on the spot:
 *   I: 25% · II: 50% · III: 100% (per item)
 * Magma Touch (capstone) always smelts, and covers everything with a furnace
 * recipe, not just ores.
 *
 * Breaks are queued and the drops are converted at the END of the same server
 * tick, after every drop entity has actually spawned — scanning during the
 * break event misses drops that spawn late.
 */
public final class MeltHandler {
	private static final Map<Item, Item> ORE_SMELTS = Map.of(
		Items.RAW_COPPER, Items.COPPER_INGOT,
		Items.RAW_IRON, Items.IRON_INGOT,
		Items.RAW_GOLD, Items.GOLD_INGOT
	);

	/** Magma Touch: everything with a furnace recipe drops pre-smelted. */
	private static final Map<Item, Item> MAGMA_SMELTS = Map.ofEntries(
		Map.entry(Items.RAW_COPPER, Items.COPPER_INGOT),
		Map.entry(Items.RAW_IRON, Items.IRON_INGOT),
		Map.entry(Items.RAW_GOLD, Items.GOLD_INGOT),
		Map.entry(Items.COBBLESTONE, Items.STONE),
		Map.entry(Items.COBBLED_DEEPSLATE, Items.DEEPSLATE),
		Map.entry(Items.STONE, Items.SMOOTH_STONE),
		Map.entry(Items.SAND, Items.GLASS),
		Map.entry(Items.RED_SAND, Items.GLASS),
		Map.entry(Items.NETHERRACK, Items.NETHER_BRICK),
		Map.entry(Items.CLAY_BALL, Items.BRICK),
		Map.entry(Items.CLAY, Items.TERRACOTTA),
		Map.entry(Items.BASALT, Items.SMOOTH_BASALT),
		Map.entry(Items.SANDSTONE, Items.SMOOTH_SANDSTONE),
		Map.entry(Items.RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE),
		Map.entry(Items.QUARTZ_BLOCK, Items.SMOOTH_QUARTZ),
		Map.entry(Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP)
	);

	private record Pending(ServerLevel level, BlockPos pos, int chancePercent, boolean magma) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private MeltHandler() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		int meltLevel = ModEnchantments.level(serverPlayer, pickaxe, ModEnchantments.MELT);
		boolean magma = ModEnchantments.level(serverPlayer, pickaxe, ModEnchantments.MAGMA_TOUCH) > 0;
		if (meltLevel <= 0 && !magma) {
			return;
		}
		int chance = switch (meltLevel) {
			case 1 -> 25;
			case 2 -> 50;
			default -> meltLevel >= 3 ? 100 : 0;
		};
		PENDING.add(new Pending(serverLevel, pos, chance, magma));
	}

	/** Called at the end of every server tick: converts the queued drops. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Pending pending : PENDING) {
			RandomSource random = pending.level().getRandom();
			for (ItemEntity drop : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(1.5), entity -> entity.tickCount <= 1)) {

				ItemStack stack = drop.getItem();
				Item result = pending.magma() ? MAGMA_SMELTS.get(stack.getItem()) : null;
				int chance = 100; // Magma Touch always smelts
				if (result == null) {
					result = ORE_SMELTS.get(stack.getItem());
					chance = pending.chancePercent();
				}
				if (result == null || chance <= 0) {
					continue;
				}

				// Roll per item so partial stacks smelt partially.
				int count = stack.getCount();
				int smelted = 0;
				for (int i = 0; i < count; i++) {
					if (random.nextInt(100) < chance) {
						smelted++;
					}
				}
				if (smelted == 0) {
					continue;
				}
				if (smelted == count) {
					drop.setItem(new ItemStack(result, count));
				} else {
					drop.setItem(new ItemStack(stack.getItem(), count - smelted));
					pending.level().addFreshEntity(new ItemEntity(
						pending.level(), drop.getX(), drop.getY(), drop.getZ(),
						new ItemStack(result, smelted)));
				}
			}
		}
		PENDING.clear();
	}
}
