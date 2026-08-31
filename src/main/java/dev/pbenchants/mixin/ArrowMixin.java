package dev.pbenchants.mixin;

import dev.pbenchants.perk.BowPerks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

/**
 * Alchemist's Quiver — tipped-arrow effects last half again as long.
 *
 * <p>Vanilla applies a tipped arrow's payload in {@code doPostHurtEffects}
 * through one call that already takes a duration scale (the arrow's own decay
 * from flying uncovered). Scaling that number is the whole node, and doing it
 * at the call site — where the victim is in hand — is what lets the PvE rule
 * apply: a player hit by the same arrow gets the vanilla duration.
 */
@Mixin(Arrow.class)
public class ArrowMixin {
	@Redirect(method = "doPostHurtEffects", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/alchemy/PotionContents;forEachEffect(Ljava/util/function/Consumer;F)V"))
	private void pbenchants$alchemistsQuiver(PotionContents contents, Consumer<MobEffectInstance> consumer,
	                                          float scale, LivingEntity target) {
		contents.forEachEffect(consumer,
			BowPerks.potionDurationScale((Arrow) (Object) this, target, scale));
	}
}
