package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Green Thumb's other half: a farmer who replants does not trample. While the
 * node is owned and a hoe is in hand, a planted crop that has not finished
 * growing refuses to break — the swing that would waste a seedling never lands,
 * and the action bar says why.
 *
 * <p>Which plants count as "still growing" is {@link HoeCrops#isGrowing}'s
 * call, so this guard and the harvest passives agree on the moment a crop is
 * ready: the farmland crops, the torchflower until it blooms, the pitcher
 * plant until it stands two blocks tall, and nether wart until its last
 * stage. Sugar cane, cocoa and berries are not guarded — cane in particular is
 * harvested by cutting the column mid-stalk, and a growth guard there would
 * fight the only way to farm it.
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
		if (!HoeCrops.isGrowing(state)) {
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
