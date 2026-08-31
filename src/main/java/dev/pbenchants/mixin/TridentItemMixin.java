package dev.pbenchants.mixin;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.CombatPerks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tidecaller II — Riptide launches on dry land.
 *
 * <p>Vanilla asks one question before a spin attack: is the thrower in water or
 * rain. Both the start of the wind-up and its release ask it, so both are
 * redirected, or the trident would refuse the charge it was about to honour.
 *
 * <p>The level is read through {@link CombatPerks#level}, which clamps to what
 * the holder has unlocked — a borrowed Tidecaller trident still needs the sky
 * to open for its owner.
 */
@Mixin(TridentItem.class)
public class TridentItemMixin {
	@Redirect(method = {"releaseUsing", "use"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
	private boolean pbenchants$tidecallerRiptide(Player player) {
		if (player.isInWaterOrRain()) {
			return true;
		}
		return CombatPerks.level(player, player.getMainHandItem(), ModEnchantments.TIDECALLER) >= 2;
	}
}
