package dev.pbenchants.mixin;

import dev.pbenchants.storage.ItemLock;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Locked Items, the screen half: a locked stack in the player's own inventory
 * ignores the two clicks that would send it away without a hand on it — the
 * shift-click that moves it into whatever container is open, and the throw
 * (Q over a slot). Every other click is the player's hand and goes through:
 * pick it up, put it down, drag it, swap it onto the hotbar, drop it by
 * clicking outside the window.
 *
 * <p>The one click it adds to rather than refuses is a locked stack set down
 * on a plain one of the same item, which vanilla merges into the slot: the
 * slot is marked first so the lock is not lost with the cursor.
 *
 * <p>Shift-click inside the plain inventory screen is left alone on purpose:
 * there it only shuttles between hotbar and backpack, and the stack never
 * leaves the bag. It is the <em>other</em> menus — a chest, a furnace, a
 * crafting table — where the same gesture is a one-way trip.
 *
 * <p>{@code doClick} runs on both sides (the client predicts every click), so
 * cancelling here keeps the two in step; the action-bar line comes from the
 * server alone. Creative mode is never second-guessed.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
	@Shadow
	@Final
	public NonNullList<Slot> slots;

	@Shadow
	public abstract ItemStack getCarried();

	@Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
	private void pbenchants$keepLockedItems(int slotId, int button, ContainerInput input, Player player,
			CallbackInfo ci) {
		if (slotId < 0 || slotId >= slots.size() || player.isCreative()) {
			return;
		}
		Slot slot = slots.get(slotId);
		// A locked stack on the cursor set down on a plain stack of the same
		// item: vanilla grows the one in the slot, so the mark would stay on
		// the cursor and vanish with it. Mark the slot first and it survives.
		if (input == ContainerInput.PICKUP) {
			ItemStack carried = getCarried();
			ItemStack inSlot = slot.getItem();
			if (ItemLock.locked(carried) && !inSlot.isEmpty() && !ItemLock.locked(inSlot)
				&& slot.mayPlace(carried) && ItemStack.isSameItemSameComponents(inSlot, carried)) {
				ItemLock.setLocked(inSlot, true);
			}
			return;
		}
		if (input != ContainerInput.QUICK_MOVE && input != ContainerInput.THROW) {
			return;
		}
		if (!(slot.container instanceof Inventory) || !ItemLock.locked(slot.getItem())) {
			return;
		}
		if (input == ContainerInput.QUICK_MOVE && (Object) this == player.inventoryMenu) {
			return;
		}
		ItemLock.refused(player);
		ci.cancel();
	}
}
