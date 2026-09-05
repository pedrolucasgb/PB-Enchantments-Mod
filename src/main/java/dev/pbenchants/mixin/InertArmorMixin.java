package dev.pbenchants.mixin;

import dev.pbenchants.perk.ArmorAuthority;
import dev.pbenchants.perk.ItemAuthority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Unearned armour protects like bare skin — the armour branch of the
 * inert-item rule, which the README owed since the Armor tree shipped.
 *
 * <p>A Protection V chestplate on a player without Aegis used to be handled
 * upstream, by rewriting it to Protection IV at the table and the anvil. That
 * hid the rank instead of gating it, and the piece worked. The rank now stays
 * on the label ({@link ItemAuthority}), and this is where it stops counting:
 * the two calls vanilla makes to turn armour into a smaller number are asked
 * again with the inert pieces left out. {@code Player} and {@code ServerPlayer}
 * override neither method, so {@code LivingEntity} is the one place to hook,
 * and the redirects sit on the call sites — {@code CombatRulesMixin} injects
 * into the callee for Sundering Blow and the two do not meet.
 *
 * <p>Mobs never reach {@link ArmorAuthority}: the tree is a player thing and a
 * zombie in a gifted chestplate keeps whatever it has.
 */
@Mixin(LivingEntity.class)
public abstract class InertArmorMixin {
	@Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
	private float pbenchants$armourOfEarnedPieces(LivingEntity target, float damage, DamageSource source,
	                                                float armor, float toughness) {
		if (target instanceof Player player) {
			float[] inert = ArmorAuthority.inertArmour(player);
			armor = Math.max(0.0F, armor - inert[0]);
			toughness = Math.max(0.0F, toughness - inert[1]);
		}
		return CombatRules.getDamageAfterAbsorb(target, damage, source, armor, toughness);
	}

	@Redirect(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
	private float pbenchants$protectionOfEarnedPieces(ServerLevel level, LivingEntity entity, DamageSource source) {
		if (entity instanceof Player player) {
			ItemStack inert = ArmorAuthority.firstInertPiece(player);
			if (!inert.isEmpty()) {
				// Being hit is the moment the player notices their armour did
				// nothing; say why, on the same cooldown as an inert swing.
				ItemAuthority.noticeInertUse(player, inert);
				return ArmorAuthority.damageProtection(level, player, source);
			}
		}
		return EnchantmentHelper.getDamageProtection(level, entity, source);
	}
}
