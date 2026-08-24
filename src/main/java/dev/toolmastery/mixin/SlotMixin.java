package dev.toolmastery.mixin;

import dev.toolmastery.perk.ItemAuthority;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The librarian half of the skill gate: the villager advertises Tool Mastery
 * books to everyone, and only an unlocked player may take one.
 *
 * <p>{@code mayPickup} is the choke point — {@code AbstractContainerMenu.doClick}
 * asks it before the plain take, the shift-click, the swap and the throw alike,
 * so a false here refuses every path, {@code onTake} never runs, and no emerald
 * is spent. Unlike the enchanting table, the rank is not clamped down: a trade
 * shows the level and the price on the label, so quietly handing over a weaker
 * book for the full price would be a bug, not a mercy.
 *
 * <p>This lives on {@link Slot} rather than {@link MerchantResultSlot} because
 * the merchant slot inherits {@code mayPickup} without overriding it, and a
 * mixin can only inject into a method the target class actually declares.
 */
@Mixin(Slot.class)
public class SlotMixin {
	@Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
	private void toolmastery$gateMasteryBooks(Player player, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof MerchantResultSlot
				&& ItemAuthority.unbuyable(player, ((Slot) (Object) this).getItem())) {
			cir.setReturnValue(false);
		}
	}
}
