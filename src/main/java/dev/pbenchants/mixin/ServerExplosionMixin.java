package dev.pbenchants.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Ablative Plating stops a creeper from throwing you — that is the node's
 * promise, kept by the {@code explosion_knockback_resistance} attribute its
 * enchantment JSON grants. A wind charge is an explosion too, and the same
 * attribute quietly ate it: a plated player could no longer wind-charge jump,
 * nor be lifted by a breeze (0.10.0 playtest). This is the one exception —
 * for an explosion whose source is a wind charge, the resistance reads as
 * zero and the burst pushes exactly as it does for everyone else.
 *
 * <p>Only the knockback is exempted. A wind charge deals no damage worth
 * speaking of, so the damage_protection half of the enchantment never had a
 * say in it either way.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Shadow
	@Final
	private Entity source;

	@Redirect(method = "hurtEntities", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double pbenchants$windIgnoresPlating(LivingEntity entity, Holder<Attribute> attribute) {
		if (source instanceof AbstractWindCharge) {
			return 0.0;
		}
		return entity.getAttributeValue(attribute);
	}
}
