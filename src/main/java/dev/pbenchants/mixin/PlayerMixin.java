package dev.pbenchants.mixin;

import dev.pbenchants.enchant.EnchanterPerks;
import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.perk.ArmorUpkeep;
import dev.pbenchants.perk.ItemAuthority;
import dev.pbenchants.perk.MiningSpeed;
import dev.pbenchants.perk.PerkAccess;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Everything that hangs off the vanilla {@code Player}: the block-breaking
 * speed modifiers (Mason's Grip, Obsidian Breaker, Lumberjack's Arms, the
 * Logic I trade-off), the Enchanter's XP hook, and the per-holder gate that
 * makes an unearned item behave like an empty hand.
 *
 * <p>The gate lives here rather than on {@code ItemStack} — where the
 * Indestructible hooks sit — because the rule is about who is holding the
 * thing, and {@code this} is the holder. See
 * {@link dev.pbenchants.perk.ItemAuthority}.
 *
 * <p>The speed part is deliberately a common mixin, not a client one: the
 * client drives the breaking animation and the server validates the break, so
 * both sides have to compute the same number or the block "heals" mid-swing.
 * Ownership therefore goes through {@link PerkAccess} instead of a straight
 * attachment read.
 *
 * <p>All the speed modifiers share a single injector on purpose. A cancellable
 * callback that calls {@code setReturnValue} returns from the method right
 * there, so a second injector on the same RETURN would silently never run.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Shadow
	protected abstract float getEnchantedDamage(Entity target, float damage, DamageSource source);

	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void pbenchants$applySpeedPassives(BlockState state, CallbackInfoReturnable<Float> cir) {
		float speed = cir.getReturnValueF();
		if (speed <= 0.0F) {
			return; // unbreakable, or the wrong tool — nothing to change
		}
		float multiplier = MiningSpeed.multiplier((Player) (Object) this, state);
		if (multiplier != 1.0F) {
			cir.setReturnValue(speed * multiplier);
		}
	}

	// ---------- Shared items: gear is only as strong as its holder ----------

	/**
	 * A locked item digs at the bare-hand rate. The redirect replaces only the
	 * <em>item's</em> contribution, not the whole method: everything vanilla
	 * layers on afterwards — haste, mining fatigue, being underwater, being in
	 * mid-air — still applies, exactly as it would to an empty hand. Cancelling
	 * the method outright would hand a borrowed pickaxe a dry-land dig speed
	 * while swimming.
	 *
	 * <p>Same two hooks as a spent Indestructible tool, one level up the stack,
	 * because this rule is about the holder rather than the item.
	 */
	@Redirect(method = "getDestroySpeed", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
	private float pbenchants$lockedDigsLikeAHand(ItemStack stack, BlockState state) {
		return ItemAuthority.locked((Player) (Object) this, stack) ? 1.0F : stack.getDestroySpeed(state);
	}

	/** ...and drops nothing from blocks that need a tool. */
	@Redirect(method = "hasCorrectToolForDrops", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;isCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean pbenchants$lockedHarvestsNothing(ItemStack stack, BlockState state) {
		return !ItemAuthority.locked((Player) (Object) this, stack) && stack.isCorrectToolForDrops(state);
	}

	/**
	 * The melee half: a locked weapon hits for bare-hand damage. Rather than
	 * reading the base attribute — which would also throw away Strength, and a
	 * potion is not the item's fault — this subtracts exactly what the item
	 * puts into ATTACK_DAMAGE. The two other contributions are cancelled just
	 * below, so nothing about the weapon survives.
	 *
	 * <p>Attack <em>speed</em> is deliberately left alone: it is read all over
	 * the place, including the client cooldown bar, and no weapon-tree
	 * enchantment exists yet to make the difference felt. Documented in the
	 * README as the open half of this decision.
	 */
	@Redirect(method = "attack", at = @At(value = "INVOKE", ordinal = 0,
		target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double pbenchants$lockedHitsLikeAHand(Player self, Holder<Attribute> attribute) {
		double value = self.getAttributeValue(attribute);
		ItemStack weapon = self.getWeaponItem();
		if (!ItemAuthority.locked(self, weapon)) {
			return value;
		}
		ItemAuthority.noticeInertUse(self, weapon);
		return value - pbenchants$weaponContribution(weapon, attribute);
	}

	/** Locked weapons carry no enchantment damage bonus either. */
	@Redirect(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/player/Player;getEnchantedDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
	private float pbenchants$lockedHasNoEnchantedDamage(Player self, Entity target, float damage, DamageSource source) {
		return ItemAuthority.locked(self, self.getWeaponItem())
			? damage
			: getEnchantedDamage(target, damage, source);
	}

	/** ...nor the per-item bonus a mace or a heavy weapon adds. */
	@Redirect(method = "attack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/Item;getAttackDamageBonus(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F"))
	private float pbenchants$lockedHasNoItemBonus(Item item, Entity target, float damage, DamageSource source) {
		return ItemAuthority.locked((Player) (Object) this, ((Player) (Object) this).getWeaponItem())
			? 0.0F
			: item.getAttackDamageBonus(target, damage, source);
	}

	/**
	 * How much of the holder's current attribute value comes from this item in
	 * the main hand. Only ADD_VALUE is summed — that is the operation every
	 * vanilla weapon uses for attack damage, and a multiplier would need the
	 * full attribute pipeline to undo correctly.
	 */
	@Unique
	private static double pbenchants$weaponContribution(ItemStack stack, Holder<Attribute> attribute) {
		double[] total = {0.0};
		stack.forEachModifier(EquipmentSlot.MAINHAND, (holder, modifier) -> {
			if (holder.equals(attribute) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				total[0] += modifier.amount();
			}
		});
		return total[0];
	}

	/**
	 * Enchanter tree: every XP point gained feeds the collect_xp gate, and the
	 * Scholar passive adds +20% per rank (rounded up, so even 1-point orbs
	 * benefit). Level deductions (negative amounts) are untouched.
	 */
	/**
	 * Bulwark III, the defender's half: the axe swing that would disable this
	 * player's shield simply does not. Redirected at the call site rather than
	 * on the method itself because the method belongs to the attacker and the
	 * enchantment belongs to the shield — here, {@code this} is the one holding
	 * it.
	 */
	@Redirect(method = "blockUsingItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/LivingEntity;getSecondsToDisableBlocking()F"))
	private float pbenchants$bulwarkRefusesTheDisable(net.minecraft.world.entity.LivingEntity attacker) {
		float seconds = attacker.getSecondsToDisableBlocking();
		Player self = (Player) (Object) this;
		if (seconds > 0.0F && ArmorPerks.enchantLevel(self.getItemBlockingWith(), ModEnchantments.BULWARK)
			>= ArmorPerks.BULWARK_UNDISABLED) {
			return 0.0F;
		}
		return seconds;
	}

	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int pbenchants$scholarXpBonus(int amount) {
		if (!((Object) this instanceof ServerPlayer serverPlayer) || amount <= 0) {
			return amount;
		}
		SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "collect_xp", amount);
		// Living Armor takes the experience before Scholar can scale it: the
		// points went into the armour, so there is nothing left to boost. One
		// injector rather than two, because two @ModifyVariable handlers on the
		// same argument would run in an order nobody declared.
		if (ArmorUpkeep.livingArmorAbsorbs(serverPlayer, amount)) {
			return 0;
		}
		int scholar = EnchanterPerks.rankedLevel(serverPlayer, EnchanterPerks.SCHOLAR);
		if (scholar > 0) {
			amount += Math.max(1, amount * scholar / 5);
		}
		return amount;
	}
}
