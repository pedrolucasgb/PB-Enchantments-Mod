package dev.pbenchants.mixin;

import dev.pbenchants.perk.BowPerks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The crossbow: how long the charge takes, and where a Multishot volley goes.
 *
 * <p>The charge duration is asked on both sides — the client for the load
 * animation and the release check, the server for the same check — so the
 * hook lives on the one method both read.
 */
@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
	/** Fletcher's Hands: the charge takes 17/29/38% less time per rank. */
	@Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
	private static void pbenchants$fletchersHands(ItemStack stack, LivingEntity shooter,
	                                               CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(BowPerks.scaledChargeDuration(shooter, cir.getReturnValueI()));
	}

	/**
	 * Multishot Focus: the volley's ±10° side arrows fire at zero instead, so
	 * all three can land on one target. The side arrows are tagged on the way
	 * out — see {@code BowPerks} for the impact-side PvP dampener that tag
	 * feeds.
	 */
	@ModifyVariable(method = "shootProjectile", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private float pbenchants$multishotFocus(float angle, LivingEntity shooter, Projectile projectile,
	                                         int index) {
		return BowPerks.focusedAngle(shooter, projectile, index, angle);
	}
}
