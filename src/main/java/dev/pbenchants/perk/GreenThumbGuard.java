package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Green Thumb's other half: a farmer who replants does not trample. While the
 * node is owned and a hoe is in hand, a farmland crop that has not finished
 * growing refuses to break — the swing that would waste a seedling never lands,
 * and the action bar says why.
 *
 * <p>Deliberately only true farmland crops ({@link CropBlock}: wheat, carrots,
 * potatoes, beetroot, torchflower, pitcher pod). Sugar cane, cocoa, berries and
 * nether wart stay out: cane in particular is harvested by cutting the column
 * mid-stalk, and a growth guard there would fight the only way to farm it.
 *
 * <p>Sneaking overrides — the same gesture that disables every other hoe and
 * axe cascade — and creative mode is never second-guessed.
 */
public final class GreenThumbGuard {
	private GreenThumbGuard() {
	}

	/** PlayerBlockBreakEvents.BEFORE: false refuses the break. */
	public static boolean allowBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BreakGuard.busy() || player.isCreative() || player.isShiftKeyDown()) {
			return true;
		}
		if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
			return true;
		}
		if (!player.getMainHandItem().is(ItemTags.HOES)) {
			return true;
		}
		if (!PerkAccess.owns(player, SkillTrees.GROUND, "green_thumb")) {
			return true;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(
				Component.translatable("msg.pbenchants.crop_still_growing"), true);
		}
		return false;
	}
}
