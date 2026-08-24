package dev.toolmastery.mixin;

import dev.toolmastery.track.EnchantTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The grindstone half of the Master Enchanter gate: taking the result of a
 * disenchant. The result slot is an anonymous class inside GrindstoneMenu, so
 * the target is spelled out; the inputs are read back off the player's open
 * menu rather than through the synthetic outer reference, and at HEAD they are
 * still there — vanilla empties them straight after.
 */
@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public class GrindstoneResultSlotMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void toolmastery$trackDisenchant(Player player, ItemStack result, CallbackInfo ci) {
		if (!(player.containerMenu instanceof GrindstoneMenu menu)) {
			return;
		}
		for (int slot = GrindstoneMenu.INPUT_SLOT; slot <= GrindstoneMenu.ADDITIONAL_SLOT; slot++) {
			ItemStack input = menu.getSlot(slot).getItem();
			if (!input.isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(input).isEmpty()) {
				EnchantTracker.onGrindstoneDisenchant(player);
				return;
			}
		}
	}
}
