package dev.pbenchants.mixin;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.CombatPerks;
import dev.pbenchants.track.CombatTracker;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Sword tree's half of {@code Player}: the damage the class adds, the
 * cooldown Nostalgy takes away, and the sweep Broad Swing hands to axes.
 *
 * <p>Kept apart from {@link PlayerMixin} on purpose. That file is about the
 * holder — dig speed, and the rule that unearned gear is inert. This one is
 * about the swing, and the two would otherwise fight over the same RETURN
 * injectors.
 *
 * <p>Everything here is common rather than client or server: the attack
 * cooldown drives the client's own cooldown bar as well as the server's damage,
 * so the two have to agree on the same number or the bar lies about the hit.
 * Ownership goes through {@link CombatPerks}, which reads the attachment on the
 * server and the synced snapshot on the client.
 */
@Mixin(Player.class)
public abstract class SwordPlayerMixin {
	@Shadow
	public abstract float getAttackStrengthScale(float partial);

	/**
	 * Nostalgy shortens the cooldown, which would make Keen Edge — a node that
	 * pays out on a <em>full</em> cooldown — a flat permanent bonus. So Keen
	 * Edge asks what the scale would have been without the node, and this flag
	 * is how it asks: the two hooks below step aside while it is set.
	 */
	@Unique
	private static boolean pbenchants$vanillaCooldown;

	// ---------- Nostalgy: the cooldown ladder ----------

	/**
	 * Ranks I to III shorten the recovery by a quarter each. Rank IV is handled
	 * by {@link #pbenchants$nostalgyScale} instead: a delay of zero would make
	 * vanilla's {@code ticker / delay} a NaN, and a NaN attack scale is a much
	 * worse bug than the one it would fix.
	 */
	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void pbenchants$nostalgyDelay(CallbackInfoReturnable<Float> cir) {
		if (pbenchants$vanillaCooldown) {
			return;
		}
		int rank = pbenchants$nostalgy();
		if (rank >= 1 && rank <= 3) {
			cir.setReturnValue(cir.getReturnValueF() * (1.0F - 0.25F * rank));
		}
	}

	/** Rank IV: the cooldown is gone. Every swing lands at full damage, 1.8-style. */
	@Inject(method = "getAttackStrengthScale", at = @At("RETURN"), cancellable = true)
	private void pbenchants$nostalgyScale(float partial, CallbackInfoReturnable<Float> cir) {
		if (!pbenchants$vanillaCooldown && pbenchants$nostalgy() >= 4) {
			cir.setReturnValue(1.0F);
		}
	}

	@Unique
	private int pbenchants$nostalgy() {
		Player self = (Player) (Object) this;
		return CombatPerks.level(self, self.getMainHandItem(), ModEnchantments.NOSTALGY);
	}

	// ---------- the damage the tree adds ----------

	/**
	 * One hook for every damage node in the class: Keen Edge, Executioner,
	 * Adrenaline, Bloodthirst and Death Eyes. This is the single point where the
	 * attacker, the target and the base damage are all in hand — and a locked
	 * item never reaches it, because {@link PlayerMixin} skips the call
	 * entirely, so a borrowed sword adds nothing here either.
	 *
	 * <p>The side effects (the mark, the cleave, the counters) deliberately only
	 * fire for the primary hit. {@code doSweepAttack} calls this again for every
	 * mob in the arc, and a sweep through six zombies should not be six marks,
	 * six cleaves and six entries in the damage gate.
	 */
	@Inject(method = "getEnchantedDamage", at = @At("RETURN"), cancellable = true)
	private void pbenchants$combatDamage(Entity target, float damage, DamageSource source,
	                                      CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		float actualScale = getAttackStrengthScale(0.5F);
		float vanillaScale = pbenchants$vanillaScale(actualScale);
		float total = cir.getReturnValueF()
			+ CombatPerks.damageBonus(self, target, damage, vanillaScale);

		// Nostalgy switched off for PvP: same swing, vanilla damage.
		if (target instanceof Player && !CombatPerks.nostalgyAppliesInPvp() && pbenchants$nostalgy() > 0) {
			total *= CombatPerks.pvpCooldownRatio(vanillaScale, actualScale);
		}
		cir.setReturnValue(total);

		if (!(self instanceof ServerPlayer player) || CombatPerks.state(self).inSweep) {
			return;
		}
		CombatPerks.onMeleeHit(player, total);
		CombatTracker.onMeleeDamage(player, total);
		if (target instanceof LivingEntity living) {
			CombatPerks.mark(player, living);
			CombatPerks.cleave(player, living, total);
		}
	}

	/** What {@code getAttackStrengthScale} would have said without Nostalgy. */
	@Unique
	private float pbenchants$vanillaScale(float actualScale) {
		if (pbenchants$nostalgy() <= 0) {
			return actualScale;
		}
		pbenchants$vanillaCooldown = true;
		try {
			return getAttackStrengthScale(0.5F);
		} finally {
			pbenchants$vanillaCooldown = false;
		}
	}

	/** The tier-1 gate wants kills that ended on a critical hit, so remember the crit. */
	@Inject(method = "crit", at = @At("HEAD"))
	private void pbenchants$rememberCrit(Entity target, CallbackInfo ci) {
		CombatPerks.onCrit((Player) (Object) this, target);
	}

	// ---------- the sweep ----------

	/**
	 * Broad Swing: an axe sweeps like a sword does. Vanilla's last word on
	 * whether a swing is a sweep is "is the main-hand item a sword", and this is
	 * that word. The enchantment half — Sweeping Edge accepting an axe at all —
	 * is the widened {@code #minecraft:enchantable/sweeping} tag, clamped back
	 * for anyone without the node at the table and the anvil.
	 */
	@Redirect(method = "isSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
	private boolean pbenchants$broadSwing(ItemStack stack, TagKey<Item> tag) {
		if (stack.is(tag)) {
			return true;
		}
		return tag == ItemTags.SWORDS && stack.is(ItemTags.AXES)
			&& CombatPerks.owns((Player) (Object) this, CombatPerks.BROAD_SWING);
	}

	@Inject(method = "doSweepAttack", at = @At("HEAD"))
	private void pbenchants$sweepStart(Entity target, float damage, DamageSource source, float scale,
	                                    CallbackInfo ci) {
		Player self = (Player) (Object) this;
		CombatPerks.state(self).inSweep = true;
		if (self instanceof ServerPlayer player) {
			CombatTracker.onSweepHit(player);
		}
	}

	@Inject(method = "doSweepAttack", at = @At("RETURN"))
	private void pbenchants$sweepEnd(Entity target, float damage, DamageSource source, float scale,
	                                  CallbackInfo ci) {
		CombatPerks.state((Player) (Object) this).inSweep = false;
	}

	/**
	 * Sweeping Arc, half one: the arc lands at 50% of a full hit at rank I and
	 * 100% at rank II. Vanilla's sweep damage is {@code 1 + ratio × damage} off
	 * the SWEEPING_DAMAGE_RATIO attribute, so the node adds to that ratio rather
	 * than to the damage — it stacks with vanilla Sweeping Edge instead of
	 * fighting it for the same number.
	 */
	@Redirect(method = "doSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double pbenchants$sweepingArcRatio(Player self, Holder<Attribute> attribute) {
		double value = self.getAttributeValue(attribute);
		if (attribute != Attributes.SWEEPING_DAMAGE_RATIO) {
			return value;
		}
		int rank = CombatPerks.level(self, self.getMainHandItem(), ModEnchantments.SWEEPING_ARC);
		return value + rank * 0.5;
	}

	/** Sweeping Arc, half two: the arc reaches one block further. */
	@Redirect(method = "doSweepAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
	private AABB pbenchants$sweepingArcReach(AABB box, double x, double y, double z) {
		return box.inflate(x + pbenchants$sweepReach(), y, z + pbenchants$sweepReach());
	}

	/** ...and vanilla's own 3-block cutoff moves with it, or the arc would find nothing. */
	@ModifyConstant(method = "doSweepAttack", constant = @Constant(doubleValue = 9.0))
	private double pbenchants$sweepingArcRange(double squared) {
		double reach = 3.0 + pbenchants$sweepReach();
		return reach * reach;
	}

	@Unique
	private double pbenchants$sweepReach() {
		Player self = (Player) (Object) this;
		return CombatPerks.level(self, self.getMainHandItem(), ModEnchantments.SWEEPING_ARC) > 0 ? 1.0 : 0.0;
	}
}
