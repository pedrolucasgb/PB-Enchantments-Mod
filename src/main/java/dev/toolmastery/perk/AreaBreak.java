package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dig Range — driven by the Dig Range enchantment on the pickaxe:
 *   I   breaks the block below the broken one
 *   II  breaks in a cross on the plane the player is facing
 *   III breaks a full 3x3 on that plane
 * Sneaking disables. Extra blocks respect tool tier and cost durability.
 */
public final class AreaBreak {
	private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

	private AreaBreak() {
	}

	public static boolean isAreaBreaking() {
		return BREAKING.get();
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BREAKING.get() || TimberScheduler.isTimberBreaking()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		int rangeLevel = ModEnchantments.level(serverPlayer, pickaxe, ModEnchantments.DIG_RANGE);
		if (rangeLevel <= 0) {
			return;
		}

		BREAKING.set(true);
		try {
			for (BlockPos target : targets(serverPlayer, pos, rangeLevel)) {
				if (pickaxeAboutToBreak(serverPlayer)) {
					return;
				}
				BlockState targetState = serverLevel.getBlockState(target);
				if (targetState.isAir()
					|| targetState.getDestroySpeed(serverLevel, target) < 0
					|| !serverPlayer.hasCorrectToolForDrops(targetState)) {
					continue;
				}
				serverPlayer.gameMode.destroyBlock(target);
			}
		} finally {
			BREAKING.set(false);
		}
	}

	private static java.util.List<BlockPos> targets(ServerPlayer player, BlockPos pos, int rangeLevel) {
		if (rangeLevel == 1) {
			return java.util.List.of(pos.below());
		}

		// Levels 2-3 work on the plane perpendicular to the player's view:
		// looking mostly up/down -> horizontal plane; otherwise the vertical
		// plane facing the player.
		boolean horizontalPlane = Math.abs(player.getXRot()) > 45.0F;
		boolean alongX = player.getDirection().getStepX() != 0;

		java.util.List<BlockPos> result = new java.util.ArrayList<>(8);
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				if (a == 0 && b == 0) {
					continue;
				}
				if (rangeLevel == 2 && a != 0 && b != 0) {
					continue; // cross shape: skip corners
				}
				BlockPos target;
				if (horizontalPlane) {
					target = pos.offset(a, 0, b);
				} else if (alongX) {
					target = pos.offset(0, a, b);
				} else {
					target = pos.offset(a, b, 0);
				}
				result.add(target);
			}
		}
		return result;
	}

	private static boolean pickaxeAboutToBreak(ServerPlayer player) {
		ItemStack pickaxe = player.getMainHandItem();
		return !pickaxe.is(ItemTags.PICKAXES)
			|| (pickaxe.isDamageableItem() && pickaxe.getDamageValue() >= pickaxe.getMaxDamage() - 2);
	}
}
