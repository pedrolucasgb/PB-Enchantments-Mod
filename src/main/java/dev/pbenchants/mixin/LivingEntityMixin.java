package dev.pbenchants.mixin;

import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.perk.CombatPerks;
import dev.pbenchants.perk.ExplorerPerks;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.track.ArmorTracker;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Five features that all hang off {@code LivingEntity}, three of them
 * sharing a single blocked hit.
 *
 * <p><b>Shield Breaker</b> (Sword tree, migrated out of the Axe tree) — axes
 * punch through a raised shield:
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
 *
 * <p><b>Shield gate tracking</b> (Armor tree) — how much a raised shield really
 * soaked, taken here because a fully blocked hit never reaches the damage
 * event at all.
 *
 * <p><b>Riposte</b> (Sword tree) — a shield raised into the swing throws a
 * quarter of the hit back at whoever landed it.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Unique
	private static final float pbenchants$EXTRA_DISABLE_SECONDS = 2.0F;

	@Unique
	private static final float pbenchants$EXTRA_DAMAGE = 2.0F;

	/** How long after raising a shield a block still counts as a parry. */
	@Unique
	private static final int pbenchants$RIPOSTE_WINDOW = 10;

	/** How much of what the shield soaked up comes back at the attacker. */
	@Unique
	private static final float pbenchants$RIPOSTE_SHARE = 0.25F;

	/** Blocks of any fall Soft Landing forgives on top of vanilla's own grace. */
	@Unique
	private static final int pbenchants$SOFT_LANDING_FREE_BLOCKS = 3;

	// 1.21.1 port: Shield Breaker, Riposte and the Armor tree's shield-block
	// tracking hung off 26.x's BlocksAttacks component and applyItemBlocking,
	// neither of which exists here. Sword and Armor are disabled on the 1.21.1
	// build, so those hooks are left out rather than rebuilt.

	// ---------- Explorer: Soft Landing and Clear Sight ----------

	/**
	 * Soft Landing, half one: the first three blocks of any fall are free. This
	 * adds to the distance vanilla already forgives — the same number Feather
	 * Falling moves — so wingsuit insurance composes with the boots instead of
	 * fighting them for the same slot.
	 */
	@ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float pbenchants$fallGrace(float distance) {
		if (!((Object) this instanceof Player player)) {
			return distance;
		}
		int free = 0;
		if (ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
			free += pbenchants$SOFT_LANDING_FREE_BLOCKS;
		}
		// Overlapping with Soft Landing on purpose, and additively rather than
		// as a max (issue #28). On 1.21.1 the grace is taken off the fall
		// distance itself, since getComfortableFallDistance is not the hook here.
		if (ArmorPerks.hasKineticPlating(player)) {
			free += ArmorPerks.KINETIC_FREE_BLOCKS;
		}
		return free > 0 ? distance - free : distance;
	}

	/**
	 * Soft Landing, half two: an elytra flown into a wall hurts half as much.
	 * Only the kinetic damage type is touched — this is insurance against a
	 * misjudged canopy, not against arrows.
	 */
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float pbenchants$scaleIncomingDamage(float amount, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self.level() instanceof ServerLevel level)) {
			return amount;
		}
		if (self instanceof Player player) {
			if (source.is(DamageTypes.FLY_INTO_WALL)
				&& ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
				amount *= 0.5F;
			}
			if (source.is(DamageTypes.FALL) && ArmorPerks.hasKineticPlating(player)) {
				amount *= ArmorPerks.KINETIC_REMAINDER;
			}
			// Beacon: Wither Ward halves what the Wither effect takes off you.
			if (source.is(DamageTypes.WITHER)
				&& dev.pbenchants.perk.BeaconPerks.owns(player, dev.pbenchants.perk.BeaconPerks.WITHER_WARD)) {
				amount *= dev.pbenchants.perk.BeaconPerks.WITHER_WARD_REMAINDER;
			}
		}
		return amount * pbenchants$guardiansAura(level, self);
	}

	/**
	 * Guardian's Aura: a player or a tamed animal standing within six blocks of
	 * someone who bought the node takes 10% less of everything.
	 *
	 * <p>It does not stack with itself — two Guardians are still 10%, not 19% —
	 * because a party of four would otherwise be a different game. The scan is
	 * the cheapest thing that answers it: nearby players only, and only for an
	 * entity that could be an ally in the first place.
	 */
	@Unique
	private float pbenchants$guardiansAura(ServerLevel level, LivingEntity victim) {
		if (!ArmorPerks.isProtectableAlly(victim)) {
			return 1.0F;
		}
		for (Player nearby : level.players()) {
			if (nearby != victim
				&& nearby.distanceToSqr(victim) <= ArmorPerks.GUARDIAN_RANGE * ArmorPerks.GUARDIAN_RANGE
				&& ArmorPerks.owns(nearby, ArmorPerks.GUARDIANS_AURA)) {
				return 1.0F - ArmorPerks.GUARDIAN_SHARE;
			}
		}
		return 1.0F;
	}

	/**
	 * Steady Stance and Warden's Weight, both on the one place vanilla applies
	 * knockback. A mob's shove is quartered; blocking with Warden's Weight
	 * cancels it outright.
	 */
	@ModifyVariable(method = "knockback(DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double pbenchants$resistKnockback(double strength) {
		if (strength <= 0.0 || !((Object) this instanceof Player player)) {
			return strength;
		}
		DamageSource source = player.getLastDamageSource();
		Entity attacker = source == null ? null : source.getEntity();
		boolean fromMob = attacker instanceof LivingEntity && !(attacker instanceof Player);
		return strength * ArmorPerks.knockbackFactor(player, fromMob);
	}

	// ---------- Enchanter: Reaper's Wisdom ----------

	/**
	 * Reaper's Wisdom: a mob killed with a Looting weapon gives more XP —
	 * +25% per level across the whole ladder, I +25% through IV +100%. Looting
	 * reaches IV in this mod (Spoils of War), and the scaling follows it there
	 * rather than stopping at the vanilla ceiling. Looting already decides how
	 * much of a mob you take away; this makes it decide how much you learn too,
	 * and gives the Enchanter a reason to care about a weapon enchantment.
	 *
	 * <p>Rounded up — a real ceiling, {@code (reward * level + 3) / 4}, not a
	 * floor with a floor of 1 under it. Plain integer division rounded the bonus
	 * <em>down</em> at every rank, so a 5-point mob under Looting I paid +1
	 * instead of the +2 that 25% of 5 comes to, and the ladder never quite added
	 * up to what it says. Scholar applies afterwards on the way into the
	 * player's bar — the two multiply.
	 */
	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void pbenchants$reapersWisdom(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		int reward = cir.getReturnValue();
		if (reward <= 0 || !(killer instanceof ServerPlayer player)
			|| !EnchanterPerks.owns(player, EnchanterPerks.REAPERS_WISDOM)) {
			return;
		}
		int looting = pbenchants$lootingLevel(level, player);
		if (looting > 0) {
			cir.setReturnValue(reward + Math.max(1, (reward * looting + 3) / 4));
		}
	}

	@Unique
	private static int pbenchants$lootingLevel(ServerLevel level, ServerPlayer player) {
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
	private int pbenchants$clearSightBreath(int air) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player && ExplorerPerks.rank(player, ExplorerPerks.CLEAR_SIGHT) >= 2) {
			int gain = air - self.getAirSupply();
			return Math.min(self.getMaxAirSupply(), air + Math.max(0, gain));
		}
		return air;
	}
}
