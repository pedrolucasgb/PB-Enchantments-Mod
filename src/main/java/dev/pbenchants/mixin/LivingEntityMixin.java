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
import net.minecraft.world.item.component.BlocksAttacks;
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
	private static final float PBENCHANTS$EXTRA_DISABLE_SECONDS = 2.0F;

	@Unique
	private static final float PBENCHANTS$EXTRA_DAMAGE = 2.0F;

	/** How long after raising a shield a block still counts as a parry. */
	@Unique
	private static final int PBENCHANTS$RIPOSTE_WINDOW = 10;

	/** How much of what the shield soaked up comes back at the attacker. */
	@Unique
	private static final float PBENCHANTS$RIPOSTE_SHARE = 0.25F;

	/** Blocks of any fall Soft Landing forgives on top of vanilla's own grace. */
	@Unique
	private static final int PBENCHANTS$SOFT_LANDING_FREE_BLOCKS = 3;

	/**
	 * Bulwark III, the defender's half: a shield that carries it is never
	 * disabled outright. Asked here rather than at the call site because
	 * {@code getSecondsToDisableBlocking} is a property of the <em>attacker</em>
	 * — see {@code PlayerMixin} for the half that knows who is blocking.
	 */
	@Inject(method = "getSecondsToDisableBlocking", at = @At("RETURN"), cancellable = true)
	private void pbenchants$shieldBreakerDuration(CallbackInfoReturnable<Float> cir) {
		float seconds = cir.getReturnValue();
		// Only lengthen a disable the axe would already cause (vanilla axes: 5s).
		if (seconds > 0.0F && (Object) this instanceof ServerPlayer attacker
			&& pbenchants$hasShieldBreaker(attacker)) {
			cir.setReturnValue(seconds + PBENCHANTS$EXTRA_DISABLE_SECONDS);
		}
	}

	/**
	 * Everything a blocked hit owes, in one injector because two of them at
	 * RETURN do not both run: the first to call setReturnValue returns there and
	 * then, and the one behind it is dead code. Three features share this hit —
	 * one from each of three trees, which is exactly why they have to be fused
	 * rather than stacked.
	 *
	 * <p>Order matters. Shield Breaker shaves the attacker's share off first;
	 * what is left is what the shield <em>really</em> stopped, and that is the
	 * number both the Armor tree's gates and Riposte's payout are scored on. A
	 * Riposte that paid out on the pre-Breaker figure would quietly undo the
	 * node it is supposed to be the counter to.
	 */
	@Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
	private void pbenchants$blockedHit(ServerLevel level, DamageSource source, float amount,
	                                     CallbackInfoReturnable<Float> cir) {
		float blocked = cir.getReturnValue();
		if (blocked <= 0.0F) {
			return;
		}
		if (source.getDirectEntity() instanceof ServerPlayer attacker
			&& pbenchants$hasShieldBreaker(attacker)) {
			blocked = Math.max(0.0F, blocked - PBENCHANTS$EXTRA_DAMAGE);
			cir.setReturnValue(blocked);
		}
		if (!((Object) this instanceof ServerPlayer defender)) {
			return;
		}
		ArmorTracker.onShieldBlock(defender, blocked);
		pbenchants$riposte(level, source, defender, blocked);
	}

	/**
	 * Riposte (Sword tree) — a shield raised just before the hit throws a
	 * quarter of it back.
	 *
	 * <p>The window is the point. A shield held up all fight is vanilla
	 * blocking; a shield raised into the swing is a parry, so the node only pays
	 * out while the block is younger than {@link #PBENCHANTS$RIPOSTE_WINDOW}
	 * ticks. {@code getTicksUsingItem} is how long the shield has been up, which
	 * is exactly the age being asked about.
	 *
	 * <p>This is the deliberate counter to Shield Breaker two tiers below it:
	 * the axe that punches through a shield and the shield that punishes the
	 * axe are the same class buying into both sides of one fight.
	 */
	@Unique
	private void pbenchants$riposte(ServerLevel level, DamageSource source, ServerPlayer defender,
	                                 float blocked) {
		if (blocked <= 0.0F || !CombatPerks.owns(defender, CombatPerks.RIPOSTE)
			|| defender.getTicksUsingItem() > PBENCHANTS$RIPOSTE_WINDOW) {
			return;
		}
		if (source.getEntity() instanceof LivingEntity attacker && attacker != defender) {
			attacker.hurtServer(level, defender.damageSources().thorns(defender),
				blocked * PBENCHANTS$RIPOSTE_SHARE);
		}
	}

	@Unique
	private static boolean pbenchants$hasShieldBreaker(ServerPlayer attacker) {
		return attacker.getMainHandItem().is(ItemTags.AXES)
			&& SkillService.owns(attacker, SkillTrees.SWORD, CombatPerks.SHIELD_BREAKER);
	}

	// ---------- Explorer: Soft Landing and Clear Sight ----------

	/**
	 * Soft Landing, half one: the first three blocks of any fall are free. This
	 * adds to the distance vanilla already forgives — the same number Feather
	 * Falling moves — so wingsuit insurance composes with the boots instead of
	 * fighting them for the same slot.
	 */
	@Inject(method = "getComfortableFallDistance", at = @At("RETURN"), cancellable = true)
	private void pbenchants$fallGrace(float base, CallbackInfoReturnable<Integer> cir) {
		if (!((Object) this instanceof Player player)) {
			return;
		}
		int free = 0;
		if (ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
			free += PBENCHANTS$SOFT_LANDING_FREE_BLOCKS;
		}
		// Overlapping with Soft Landing on purpose, and additively rather than
		// as a max: issue #28 asked for the two to stack to something sane, and
		// nine free blocks for a player who bought both classes is exactly that
		// — generous, still short of the fall that actually kills you.
		if (ArmorPerks.hasKineticPlating(player)) {
			free += ArmorPerks.KINETIC_FREE_BLOCKS;
		}
		if (free > 0) {
			cir.setReturnValue(cir.getReturnValue() + free);
		}
	}

	/**
	 * Soft Landing, half two: an elytra flown into a wall hurts half as much.
	 * Only the kinetic damage type is touched — this is insurance against a
	 * misjudged canopy, not against arrows.
	 */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float pbenchants$scaleIncomingDamage(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (self instanceof Player player) {
			if (source.is(DamageTypes.FLY_INTO_WALL)
				&& ExplorerPerks.owns(player, ExplorerPerks.SOFT_LANDING)) {
				amount *= 0.5F;
			}
			if (source.is(DamageTypes.FALL) && ArmorPerks.hasKineticPlating(player)) {
				amount *= ArmorPerks.KINETIC_REMAINDER;
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
	@ModifyVariable(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
		at = @At("HEAD"), argsOnly = true)
	private float pbenchants$resistKnockback(float strength, double x, double y, double z, DamageSource source) {
		if (strength <= 0.0F || !((Object) this instanceof Player player)) {
			return strength;
		}
		Entity attacker = source.getEntity();
		boolean fromMob = attacker instanceof LivingEntity && !(attacker instanceof Player);
		return strength * ArmorPerks.knockbackFactor(player, fromMob);
	}

	/**
	 * Shield Wall I: the shield is up the moment you raise it. Vanilla makes
	 * you hold it for five ticks first, and those five ticks are the whole
	 * reason blocking feels late.
	 */
	@Redirect(method = "getItemBlockingWith", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/component/BlocksAttacks;blockDelayTicks()I"))
	private int pbenchants$shieldWallDelay(BlocksAttacks component) {
		if ((Object) this instanceof Player player && ArmorPerks.rank(player, ArmorPerks.SHIELD_WALL) >= 1) {
			return 0;
		}
		return component.blockDelayTicks();
	}

	/**
	 * Shield Wall II: a wider arc. The angle handed to the component is how far
	 * off-centre the hit came from, and reporting a smaller one is what makes
	 * the cone wider — the component's own thresholds stay untouched, so a
	 * shield with unusual data still behaves like itself.
	 */
	@ModifyArg(method = "applyItemBlocking", index = 2, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"))
	private double pbenchants$shieldWallArc(double angle) {
		if ((Object) this instanceof Player player && ArmorPerks.rank(player, ArmorPerks.SHIELD_WALL) >= 2) {
			return angle * ArmorPerks.SHIELD_WALL_ARC;
		}
		return angle;
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
	private void pbenchants$reapersWisdom(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		int reward = cir.getReturnValue();
		if (reward <= 0 || !(killer instanceof ServerPlayer player)
			|| !EnchanterPerks.owns(player, EnchanterPerks.REAPERS_WISDOM)) {
			return;
		}
		int looting = pbenchants$lootingLevel(level, player);
		if (looting > 0) {
			cir.setReturnValue(reward + Math.max(1, reward * looting / 4));
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
