package dev.toolmastery.mixin;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shield Breaker — axes punch through a raised shield:
 *   +2s on the shield cooldown the axe already inflicts
 *   +2 damage that the shield fails to soak up
 *
 * Both hooks are server-side: getSecondsToDisableBlocking is asked of the
 * attacker in Player#blockUsingItem, and applyItemBlocking returns the amount
 * the defender's shield absorbs — the caller subtracts it from the damage, so
 * absorbing 2 less is the same as hitting 2 harder.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Unique
	private static final float TOOLMASTERY$EXTRA_DISABLE_SECONDS = 2.0F;

	@Unique
	private static final float TOOLMASTERY$EXTRA_DAMAGE = 2.0F;

	@Inject(method = "getSecondsToDisableBlocking", at = @At("RETURN"), cancellable = true)
	private void toolmastery$shieldBreakerDuration(CallbackInfoReturnable<Float> cir) {
		float seconds = cir.getReturnValue();
		// Only lengthen a disable the axe would already cause (vanilla axes: 5s).
		if (seconds > 0.0F && (Object) this instanceof ServerPlayer attacker
			&& toolmastery$hasShieldBreaker(attacker)) {
			cir.setReturnValue(seconds + TOOLMASTERY$EXTRA_DISABLE_SECONDS);
		}
	}

	@Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
	private void toolmastery$shieldBreakerDamage(ServerLevel level, DamageSource source, float amount,
	                                             CallbackInfoReturnable<Float> cir) {
		float blocked = cir.getReturnValue();
		if (blocked > 0.0F && source.getDirectEntity() instanceof ServerPlayer attacker
			&& toolmastery$hasShieldBreaker(attacker)) {
			cir.setReturnValue(Math.max(0.0F, blocked - TOOLMASTERY$EXTRA_DAMAGE));
		}
	}

	@Unique
	private static boolean toolmastery$hasShieldBreaker(ServerPlayer attacker) {
		return attacker.getMainHandItem().is(ItemTags.AXES)
			&& SkillService.owns(attacker, SkillTrees.AXE, "shield_breaker");
	}
}
