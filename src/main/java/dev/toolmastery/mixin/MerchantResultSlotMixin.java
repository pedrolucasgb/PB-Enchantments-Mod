package dev.toolmastery.mixin;

import dev.toolmastery.track.EnchantTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enchanted books bought from a villager. Same hook as the crafting and furnace
 * result slots: checkTakeAchievements sees a stack with real identity in every
 * take path, and removeCount holds how many actually left the slot.
 */
@Mixin(MerchantResultSlot.class)
public class MerchantResultSlotMixin {
	@Shadow
	@Final
	private Player player;

	@Shadow
	private int removeCount;

	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void toolmastery$trackTrade(ItemStack stack, CallbackInfo ci) {
		if (removeCount > 0) {
			EnchantTracker.onMerchantTake(player, stack, removeCount);
		}
	}
}
