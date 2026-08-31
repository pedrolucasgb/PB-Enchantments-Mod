package dev.pbenchants.client.mixin;

import dev.pbenchants.enchant.EnchanterPerks;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The client half of Anvil Adept II.
 *
 * <p>The server already keeps the result item in the slot at a capped 40
 * levels, but the screen decides on its own that any bill of 40 or more reads
 * "Too Expensive!" in red. Answering the same question the server does — does
 * this player own the node — makes the label say <em>Enchantment Cost: 40</em>
 * instead, which is what the anvil is actually going to charge.
 *
 * <p>Cosmetic only: the price and whether the item can be taken are both the
 * server's answer, synced through the menu's cost data slot.
 */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
	@Redirect(method = "extractLabels", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
	private boolean pbenchants$anvilMasterIsNeverTooExpensive(LocalPlayer player) {
		return player.hasInfiniteMaterials() || EnchanterPerks.owns(player, EnchanterPerks.ANVIL_MASTER);
	}
}
