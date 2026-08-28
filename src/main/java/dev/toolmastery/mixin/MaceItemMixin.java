package dev.toolmastery.mixin;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.perk.CombatPerks;
import dev.toolmastery.track.CombatTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gravity Well — the mace counts more of your drop.
 *
 * <p>Vanilla turns a fall into damage in {@code getAttackDamageBonus}, so the
 * node scales what comes out of it rather than re-deriving the fall: Density
 * and the smash formula keep working, and the enchantment only ever makes a
 * smash that already happened bigger. That also means a locked mace is
 * unaffected for free — {@link PlayerMixin} zeroes this call for gear its
 * holder has not earned, so the hook is never reached.
 *
 * <p>The tier-5 gate lives here too, because the fall distance that made a slam
 * a slam is only knowable at the moment of the hit.
 */
@Mixin(MaceItem.class)
public class MaceItemMixin {
	/** How high a fall has to be before the gate counts it as a slam. */
	private static final double SLAM_BLOCKS = 5.0;

	/** How much more of the drop each rank of Gravity Well converts. */
	private static final float PER_RANK = 0.25F;

	@Inject(method = "getAttackDamageBonus", at = @At("RETURN"), cancellable = true)
	private void toolmastery$gravityWell(Entity target, float damage, DamageSource source,
	                                     CallbackInfoReturnable<Float> cir) {
		float smash = cir.getReturnValueF();
		if (smash <= 0.0F || !(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (player.fallDistance >= SLAM_BLOCKS) {
			CombatTracker.onMaceSlam(player);
		}
		int rank = CombatPerks.level(player, player.getMainHandItem(), ModEnchantments.GRAVITY_WELL);
		if (rank > 0 && CombatPerks.appliesTo(target)) {
			cir.setReturnValue(smash * (1.0F + PER_RANK * rank));
		}
	}
}
