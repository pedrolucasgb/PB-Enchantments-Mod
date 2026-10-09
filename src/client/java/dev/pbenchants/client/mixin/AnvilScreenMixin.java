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
	// 1.21.1: the "Too Expensive!" check in renderLabels reads Abilities.instabuild.
	@Redirect(method = "renderLabels", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z"))
	private boolean pbenchants$anvilMasterIsNeverTooExpensive(net.minecraft.world.entity.player.Abilities abilities) {
		LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
		return abilities.instabuild
			|| (player != null && EnchanterPerks.owns(player, EnchanterPerks.ANVIL_MASTER));
	}
}
