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

	@Shadow
	protected abstract boolean canCriticalAttack(Entity target);

	// ---------- Nostalgy: the cooldown ladder ----------

	/**
	 * Ranks I to III shorten the recovery by a quarter each. Rank IV is handled
	 * by {@link #pbenchants$nostalgyScale} instead: a delay of zero would make
	 * vanilla's {@code ticker / delay} a NaN, and a NaN attack scale is a much
	 * worse bug than the one it would fix.
	 *
	 * <p>Both hooks step aside while {@code CombatPerks.vanillaCooldownQuery()}
	 * is set — that is Keen Edge asking what the cooldown would have been
	 * without Nostalgy, so a timing reward does not become a flat bonus.
	 */
	@Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
	private void pbenchants$nostalgyDelay(CallbackInfoReturnable<Float> cir) {
		if (CombatPerks.vanillaCooldownQuery()) {
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
		if (!CombatPerks.vanillaCooldownQuery() && pbenchants$nostalgy() >= 4) {
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
	 * Every damage node in the class — Keen Edge, Executioner, Adrenaline,
	 * Bloodthirst, Death Eyes — funnels through
	 * {@link CombatPerks#onEnchantedDamage}. A locked item never reaches it,
	 * because {@link PlayerMixin} skips the call entirely, so a borrowed sword
	 * adds nothing here either.
	 *
	 * <p>This injector only ever fires for the <b>client's</b> predicted swing:
	 * {@code ServerPlayer} overrides {@code getEnchantedDamage} without calling
	 * super, so the server's copy of the same hook lives in
	 * {@link SwordServerPlayerMixin}. Keeping this half keeps the client's
	 * sounds and particles in step with the damage the server will deal.
	 */
	@Inject(method = "getEnchantedDamage", at = @At("RETURN"), cancellable = true)
	private void pbenchants$combatDamage(Entity target, float damage, DamageSource source,
	                                      CallbackInfoReturnable<Float> cir) {
		Player self = (Player) (Object) this;
		float actualScale = getAttackStrengthScale(0.5F);
		float vanillaScale = CombatPerks.vanillaAttackScale(self, actualScale);
		cir.setReturnValue(CombatPerks.onEnchantedDamage(self, target, damage,
			cir.getReturnValueF(), actualScale, vanillaScale));
	}

	// ---------- the counters the fight feeds ----------

	/**
	 * The tier-1 gate wants kills that ended on a critical hit, so the crit is
	 * remembered off vanilla's own crit <em>decision</em>, mid-swing. Not off
	 * {@code Player.crit}: that is the particle call, it happens after the
	 * damage — after the death event, for the killing blow — and
	 * {@code ServerPlayer} overrides it without calling super anyway, so a hook
	 * there never fired on the server at all.
	 */
	@Redirect(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean pbenchants$rememberCrit(Player self, Entity target) {
		boolean crit = canCriticalAttack(target);
		if (crit && self instanceof ServerPlayer) {
			CombatPerks.onCrit(self, target);
		}
		return crit;
	}

	/**
	 * ...and every swing starts by forgetting the last one, or a single crit
	 * would make every later kill of that mob a "crit kill". Vanilla skips the
	 * crit check entirely for weak swings, so the forgetting cannot live in the
	 * redirect above.
	 */
	@Inject(method = "attack", at = @At("HEAD"))
	private void pbenchants$forgetCrit(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (self instanceof ServerPlayer) {
			CombatPerks.forgetCrit(self);
		}
	}

	/**
	 * The melee-damage gate counts what the swing actually cost the target —
	 * read where vanilla reads its own DAMAGE_DEALT stat, so the counter, the
	 * stats screen and the player's sense of the fight all agree. Both melee
	 * paths ({@code attack} and the spear's {@code stabAttack}) land here, only
	 * on hits that connected, with the cooldown, the crit and the target's
	 * remaining health already spoken for.
	 */
	@Inject(method = "damageStatsAndHearts", at = @At("HEAD"))
	private void pbenchants$countMeleeDamage(Entity target, float healthBefore, CallbackInfo ci) {
		if ((Player) (Object) this instanceof ServerPlayer player && target instanceof LivingEntity living) {
			CombatTracker.onMeleeDamage(player, healthBefore - living.getHealth());
		}
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
