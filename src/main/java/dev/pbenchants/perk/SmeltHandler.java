package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
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
 * Smelt — chance to smelt ore drops on the spot:
 *   I: 25% · II: 50% · III: 100% (per item)
 *
 * Breaks are queued and the drops are converted at the END of the same server
 * tick, after every drop entity has actually spawned — scanning during the
 * break event misses drops that spawn late.
 */
public final class SmeltHandler {
	private static final Map<Item, Item> ORE_SMELTS = Map.of(
		Items.RAW_COPPER, Items.COPPER_INGOT,
		Items.RAW_IRON, Items.IRON_INGOT,
		Items.RAW_GOLD, Items.GOLD_INGOT
	);

	private record Pending(ServerLevel level, BlockPos pos, int chancePercent) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private SmeltHandler() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		int smeltLevel = ItemAuthority.effectiveLevel(serverPlayer, pickaxe, ModEnchantments.SMELT);
		if (smeltLevel <= 0) {
			return;
		}
		int chance = switch (smeltLevel) {
			case 1 -> 25;
			case 2 -> 50;
			default -> smeltLevel >= 3 ? 100 : 0;
		};
		PENDING.add(new Pending(serverLevel, pos, chance));
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
				Item result = ORE_SMELTS.get(stack.getItem());
				int chance = pending.chancePercent();
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
