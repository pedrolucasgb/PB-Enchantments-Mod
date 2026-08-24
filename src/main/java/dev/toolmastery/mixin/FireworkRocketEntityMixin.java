package dev.toolmastery.mixin;

import dev.toolmastery.perk.Slipstream;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Slipstream — a rocket boosting a player with the enchantment keeps pushing
 * for longer.
 *
 * <p>The extension is applied once, on the rocket's very first tick, so the
 * number is stable for the rest of its life and a rank change mid-flight cannot
 * make a rocket flicker. Rockets fired at the sky, at a crossbow target or by a
 * dispenser have no attached entity and fall straight through.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
	@Shadow
	private int life;

	@Shadow
	private int lifetime;

	@Shadow
	private LivingEntity attachedToEntity;

	@Unique
	private boolean toolmastery$stretched;

	@Inject(method = "tick", at = @At("HEAD"))
	private void toolmastery$slipstream(CallbackInfo ci) {
		if (toolmastery$stretched || life > 0) {
			return;
		}
		toolmastery$stretched = true;
		if (attachedToEntity instanceof Player player) {
			lifetime += Slipstream.bonusLifetime(player, lifetime);
		}
	}
}
