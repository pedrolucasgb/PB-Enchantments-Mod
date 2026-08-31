package dev.pbenchants.mixin;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.CombatPerks;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sundering Blow — the target's armour counts for 20% or 40% less.
 *
 * <p>Armour is applied in exactly one place, and this is it: every damage path
 * in the game funnels through {@code getDamageAfterAbsorb}. Shaving the armour
 * on the way in means the node composes with Protection, Resistance and
 * absorption instead of racing them, and no second formula has to be kept in
 * step with vanilla's — the answer is vanilla's own, asked again with a smaller
 * armour value. The re-entry guard is what keeps that from being a loop; the
 * damage pipeline is single-threaded per side, so a plain flag is enough.
 *
 * <p><b>PvE-only.</b> An armour-piercing enchantment on a server is a different
 * conversation from one in a single-player world, and this tree answered it:
 * against another player the armour is untouched. {@code pvp_perks} is the
 * switch for servers that disagree.
 */
@Mixin(CombatRules.class)
public class CombatRulesMixin {
	/** How much of the armour each rank ignores. */
	@Unique
	private static final float pbenchants$PER_RANK = 0.2F;

	/** True while the recomputation below is in flight. */
	@Unique
	private static boolean pbenchants$sundering;

	@Inject(method = "getDamageAfterAbsorb", at = @At("HEAD"), cancellable = true)
	private static void pbenchants$sunderArmour(LivingEntity target, float damage, DamageSource source,
	                                             float armor, float toughness,
	                                             CallbackInfoReturnable<Float> cir) {
		if (pbenchants$sundering || armor <= 0.0F
			|| !(source.getEntity() instanceof Player attacker)
			|| !CombatPerks.appliesTo(target)) {
			return;
		}
		int rank = CombatPerks.level(attacker, attacker.getMainHandItem(), ModEnchantments.SUNDERING_BLOW);
		if (rank <= 0) {
			return;
		}
		pbenchants$sundering = true;
		try {
			cir.setReturnValue(CombatRules.getDamageAfterAbsorb(target, damage, source,
				armor * (1.0F - pbenchants$PER_RANK * rank), toughness));
		} finally {
			pbenchants$sundering = false;
		}
	}
}
