package dev.toolmastery.mixin;

import dev.toolmastery.track.EnchantTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The anvil half of the Arcanist gate. Only a real merge counts: renaming or
 * repairing with raw material leaves the sacrifice slot without enchantments,
 * and those takes are ignored. Injected at HEAD because onTake is where vanilla
 * clears the inputs — a tick later they are already gone. The inputs are read
 * off the player's open menu because inputSlots lives on ItemCombinerMenu, and
 * a @Shadow only reaches fields declared in the target class itself.
 */
@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void toolmastery$trackCombine(Player player, ItemStack result, CallbackInfo ci) {
		if (!(player.containerMenu instanceof AnvilMenu menu)) {
			return;
		}
		ItemStack sacrifice = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (!sacrifice.isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(sacrifice).isEmpty()) {
			EnchantTracker.onAnvilCombine(player);
		}
	}
}
