package dev.toolmastery.mixin;

import dev.toolmastery.perk.CombatPerks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Sword tree's damage hook, server edition — and the one that actually
 * decides a fight. {@code ServerPlayer} <b>overrides</b>
 * {@code getEnchantedDamage} and goes straight to
 * {@code EnchantmentHelper.modifyDamage} without calling super, so
 * {@link SwordPlayerMixin}'s injector on {@code Player} never runs on the
 * server: every damage node, Adrenaline's ramp, Hunter's Mark, Cleave and the
 * melee counters were silently dead in real play until this mixin gave the
 * override its own copy of the hook.
 *
 * <p>Both injectors call the same {@link CombatPerks#onEnchantedDamage}, so
 * the client's predicted swing and the server's real one cannot drift apart —
 * one of them is just never the one that deals the damage.
 */
@Mixin(ServerPlayer.class)
public abstract class SwordServerPlayerMixin {
	@Inject(method = "getEnchantedDamage", at = @At("RETURN"), cancellable = true)
	private void toolmastery$combatDamage(Entity target, float damage, DamageSource source,
	                                      CallbackInfoReturnable<Float> cir) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		float actualScale = self.getAttackStrengthScale(0.5F);
		float vanillaScale = CombatPerks.vanillaAttackScale(self, actualScale);
		cir.setReturnValue(CombatPerks.onEnchantedDamage(self, target, damage,
			cir.getReturnValueF(), actualScale, vanillaScale));
	}
}
