package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Harvest Swing — the hoe's area harvest:
 *   I 3x3 · II 5x5 · III 7x7, on the flat around the crop you broke.
 *
 * <p>Only fully grown plants come up, which is what keeps a 7x7 honest: a field
 * ripens unevenly, so the widest swing takes what is ready and leaves the rest
 * to finish. Sneaking disables, as everywhere else in the mod.
 *
 * <p>No floor rule and no hardness budget here, unlike its shovel counterpart: a
 * field is one plane by construction, and crops break instantly. Replanting
 * composes for free — every extra block goes through {@code destroyBlock}, which
 * re-enters the break event, so {@link HoeHarvest} queues each one on its own.
 */
public final class HoeAreaHarvest {
	private HoeAreaHarvest() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BreakGuard.busy()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return;
		}
		ItemStack hoe = serverPlayer.getMainHandItem();
		if (!hoe.is(ItemTags.HOES) || !HoeCrops.isHarvestable(state)) {
			return;
		}
		int swingLevel = ItemAuthority.effectiveLevel(serverPlayer, hoe, ModEnchantments.HARVEST_SWING);
		if (swingLevel <= 0) {
			return;
		}
		int radius = Math.min(swingLevel, 3);

		int taken = 0;
		BreakGuard.enter();
		try {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dz == 0) {
						continue;
					}
					if (hoeAboutToBreak(serverPlayer)) {
						return;
					}
					BlockPos target = pos.offset(dx, 0, dz);
					if (!HoeCrops.isHarvestable(serverLevel.getBlockState(target))) {
						continue;
					}
					if (serverPlayer.gameMode.destroyBlock(target)) {
						taken++;
					}
				}
			}
		} finally {
			BreakGuard.exit();
			if (taken > 0) {
				SkillService.addCount(serverPlayer, SkillTrees.GROUND, "harvest_with_swing", taken);
			}
		}
	}

	private static boolean hoeAboutToBreak(ServerPlayer player) {
		ItemStack hoe = player.getMainHandItem();
		return !hoe.is(ItemTags.HOES)
			|| (hoe.isDamageableItem() && hoe.getDamageValue() >= hoe.getMaxDamage() - 2);
	}
}
