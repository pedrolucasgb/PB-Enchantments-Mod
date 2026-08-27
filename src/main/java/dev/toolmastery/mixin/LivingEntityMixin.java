package dev.toolmastery.mixin;

import dev.toolmastery.enchant.EnchanterPerks;
import dev.toolmastery.perk.ExplorerPerks;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Three unrelated features that all hang off {@code LivingEntity}.
 *
 * <p><b>Shield Breaker</b> (Axe tree) — axes punch through a raised shield:
 * +2s on the shield cooldown the axe already inflicts, and +2 damage the shield
 * fails to soak up. Both hooks are server-side: getSecondsToDisableBlocking is
 * asked of the attacker in Player#blockUsingItem, and applyItemBlocking returns
 * the amount the defender's shield absorbs — the caller subtracts it from the
 * damage, so absorbing 2 less is the same as hitting 2 harder.
 *
 * <p><b>Soft Landing and Clear Sight</b> (Explorer tree) — see the individual
 * methods.
 *
 * <p><b>Reaper's Wisdom</b> (Enchanter tree) — mob XP scales with the Looting
 * on the weapon that landed the kill.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Unique
	private static final float TOOLMASTERY$EXTRA_DISABLE_SECONDS = 2.0F;

	@Unique
	private static final float TOOLMASTERY$EXTRA_DAMAGE = 2.0F;

	/** Blocks of any fall Soft Landing forgives on top of vanilla's own grace. */
	@Unique
	private static final int TOOLMASTERY$SOFT_LANDING_FREE_BLOCKS = 3;

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

	// ---------- Explorer: Soft Landing and Clear Sight ----------

	/**
	 * Soft Landing, half one: the first three blocks of any fall are free. This
	 * adds to the distance vanilla already forgives — the same number Feather
	 * Falling moves — so wingsuit insurance composes with the boots instead of
	 * fighting them for the same slot.
	 */
	@Inject(method = "getComfortableFallDistance", at = @At("RETURN"), cancellable = true)
	private void toolmastery$softLandingFallGrace(float base, CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof Player player
			&& ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
			cir.setReturnValue(cir.getReturnValue() + TOOLMASTERY$SOFT_LANDING_FREE_BLOCKS);
		}
	}

	/**
	 * Soft Landing, half two: an elytra flown into a wall hurts half as much.
	 * Only the kinetic damage type is touched — this is insurance against a
	 * misjudged canopy, not against arrows.
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float toolmastery$softLandingKinetic(float amount, ServerLevel level, DamageSource source) {
		if (source.is(DamageTypes.FLY_INTO_WALL) && (Object) this instanceof Player player
			&& ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
			return amount * 0.5F;
		}
		return amount;
	}

	// ---------- Enchanter: Reaper's Wisdom ----------

	/**
	 * Reaper's Wisdom: a mob killed with a Looting weapon gives more XP —
	 * +25% per level, so Looting III is +75%. Looting already decides how much
	 * of a mob you take away; this makes it decide how much you learn too, and
	 * gives the Enchanter a reason to care about a weapon enchantment.
	 *
	 * <p>Rounded up, so a 1-point kill still benefits, and the Scholar bonus
	 * applies afterwards on the way into the player's bar — the two multiply.
	 */
	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void toolmastery$reapersWisdom(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		int reward = cir.getReturnValue();
		if (reward <= 0 || !(killer instanceof ServerPlayer player)
			|| !EnchanterPerks.owns(player, EnchanterPerks.REAPERS_WISDOM)) {
			return;
		}
		int looting = toolmastery$lootingLevel(level, player);
		if (looting > 0) {
			cir.setReturnValue(reward + Math.max(1, reward * looting / 4));
		}
	}

	@Unique
	private static int toolmastery$lootingLevel(ServerLevel level, ServerPlayer player) {
		Holder<Enchantment> looting = level.registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(Enchantments.LOOTING)
			.orElse(null);
		return looting == null ? 0 : EnchantmentHelper.getEnchantmentLevel(looting, player);
	}

	/**
	 * Clear Sight II: breath comes back about twice as fast once you surface.
	 * The return value is the new air level, so doubling the <em>gain</em> —
	 * not the level — is what "twice as fast" means here.
	 */
	@ModifyVariable(method = "increaseAirSupply", at = @At("RETURN"))
	private int toolmastery$clearSightBreath(int air) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && ExplorerPerks.rank(player, ExplorerPerks.CLEAR_SIGHT) >= 2) {
			int gain = air - self.getAirSupply();
			return Math.min(self.getMaxAirSupply(), air + Math.max(0, gain));
		}
		return air;
	}
}
