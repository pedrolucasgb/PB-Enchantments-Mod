package dev.pbenchants.mixin;

import dev.pbenchants.perk.ArrowOrigin;
import dev.pbenchants.perk.BowPerks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Bow tree's half of the arrow itself: where it was loosed, what it is
 * worth when it lands, how hard gravity pulls it, and whether it comes back.
 *
 * <p>The damage redirect is the ranged mirror of {@code SwordPlayerMixin}'s
 * getEnchantedDamage hook — the one point where the arrow, its shooter, the
 * weapon it left and the target are all in hand. Routing vanilla's own
 * enchantment pass through {@link BowPerks#arrowDamage} is also what makes an
 * unearned bow <em>inert</em>: a locked weapon skips the pass and the arrow
 * lands at bare-bow damage, no Power, no Flame teeth behind it.
 *
 * <p>ThrownTrident overrides onHitEntity without calling super, so nothing
 * here touches tridents — those belong to the Sword tree.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements ArrowOrigin {
	/** Where this arrow was loosed. Not saved: see {@link ArrowOrigin}. */
	@Unique
	@Nullable
	private Vec3 pbenchants$origin;

	/** One recovery roll per arrow, however many paths lead to it being gone. */
	@Unique
	private boolean pbenchants$recoveryRolled;

	@Override
	@Nullable
	public Vec3 pbenchants$origin() {
		return pbenchants$origin;
	}

	/** The launch point, remembered the moment the shot gets its velocity. */
	@Inject(method = "shoot(DDDFF)V", at = @At("HEAD"))
	private void pbenchants$rememberOrigin(double x, double y, double z, float velocity, float inaccuracy,
	                                        CallbackInfo ci) {
		pbenchants$origin = ((AbstractArrow) (Object) this).position();
	}

	/**
	 * Every damage node of the class, plus the inert-weapon rule, in one
	 * redirect — vanilla's enchantment pass goes through {@code BowPerks},
	 * which decides whether to run it at all and what the tree adds on top.
	 */
	@Redirect(method = "onHitEntity", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
	private float pbenchants$arrowDamage(ServerLevel level, ItemStack weapon, Entity target,
	                                      DamageSource source, float base) {
		return BowPerks.arrowDamage(level, (AbstractArrow) (Object) this, pbenchants$origin,
			weapon, target, source, base);
	}

	/** Gale: a flatter arc is more range and less lead to guess. */
	@Inject(method = "getDefaultGravity", at = @At("RETURN"), cancellable = true)
	private void pbenchants$gale(CallbackInfoReturnable<Double> cir) {
		double factor = BowPerks.galeGravityFactor((AbstractArrow) (Object) this);
		if (factor < 1.0) {
			cir.setReturnValue(cir.getReturnValueD() * factor);
		}
	}

	/**
	 * Arrow Recovery, path one: the arrow buried itself in something and
	 * vanished — pierce exhausted, mob killed, slime bounced. Vanilla discards
	 * it inside onHitEntity, so the roll happens right after.
	 */
	@Inject(method = "onHitEntity", at = @At("RETURN"))
	private void pbenchants$recoverAfterHit(EntityHitResult hit, CallbackInfo ci) {
		pbenchants$maybeRecover();
	}

	/** Arrow Recovery, path two: the arrow aged out in the terrain it hit. */
	@Inject(method = "tickDespawn", at = @At("RETURN"))
	private void pbenchants$recoverOnDespawn(CallbackInfo ci) {
		pbenchants$maybeRecover();
	}

	@Unique
	private void pbenchants$maybeRecover() {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (pbenchants$recoveryRolled || !self.isRemoved() || self.level().isClientSide()) {
			return;
		}
		pbenchants$recoveryRolled = true;
		BowPerks.onArrowGone(self);
	}
}
