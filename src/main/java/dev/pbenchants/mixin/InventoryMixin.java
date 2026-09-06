package dev.pbenchants.mixin;

import dev.pbenchants.storage.ItemLock;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Locked Items, the drop-key half. {@code removeFromSelected} is what the drop
 * key calls on both sides — the client to predict, the server to do — so an
 * empty answer here keeps a locked stack in the hand on both, and the packet
 * the client still sends finds nothing to drop on the server either.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Shadow
	@Final
	public Player player;

	@Shadow
	public abstract ItemStack getSelectedItem();

	@Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
	private void pbenchants$keepLockedInHand(boolean wholeStack, CallbackInfoReturnable<ItemStack> cir) {
		if (player.isCreative() || !ItemLock.locked(getSelectedItem())) {
			return;
		}
		ItemLock.refused(player);
		cir.setReturnValue(ItemStack.EMPTY);
	}
}
