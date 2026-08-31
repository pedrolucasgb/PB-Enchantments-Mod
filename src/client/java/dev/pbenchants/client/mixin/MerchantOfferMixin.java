package dev.pbenchants.client.mixin;

import dev.pbenchants.perk.ItemAuthority;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of the librarian gate: a book this player has not unlocked shows
 * up barred, rather than clicking through into a refusal.
 *
 * <p>Reporting the offer as out of stock is the whole trick — {@code
 * MerchantScreen} already draws the crossed-out arrow for those, and
 * {@code MerchantContainer} already refuses to fill the result slot, so no
 * rendering code has to learn about skill trees. The offer itself is untouched
 * and shared: two players at the same librarian see the same book, and only the
 * unlocked one can take it. The server stays the authority
 * ({@link dev.pbenchants.mixin.SlotMixin}); this only spares the click.
 *
 * <p>Guarded on the render thread because single-player runs the integrated
 * server in this same JVM, where the mixin would otherwise also answer for the
 * villager's own trade logic.
 */
@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {
	@Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
	private void pbenchants$barLockedBooks(CallbackInfoReturnable<Boolean> cir) {
		Minecraft client = Minecraft.getInstance();
		if (!client.isSameThread()) {
			return;
		}
		LocalPlayer player = client.player;
		if (player != null && ItemAuthority.unbuyable(player, ((MerchantOffer) (Object) this).getResult())) {
			cir.setReturnValue(true);
		}
	}
}
