package dev.pbenchants.mixin;

import dev.pbenchants.track.ItemGainTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks checkTakeAchievements instead of onTake: on shift-click the stack
 * passed to onTake is already drained (reports AIR), while this method always
 * receives a stack with real identity and removeCount holds the exact amount
 * taken — the same bookkeeping vanilla uses for furnace XP and stats.
 */
@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {
	@Shadow
	@Final
	private Player player;

	@Shadow
	private int removeCount;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void pbenchants$trackSmelt(ItemStack stack, CallbackInfo ci) {
		if (removeCount > 0) {
			ItemGainTracker.onSmeltTake(player, stack, removeCount);
		}
	}
}
