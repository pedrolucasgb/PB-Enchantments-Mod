package dev.toolmastery.mixin;

import dev.toolmastery.perk.BowPerks;
import dev.toolmastery.track.BowTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * The funnel every ranged shot passes through — bow and crossbow both end in
 * {@code ProjectileWeaponItem.shoot}, which makes it the one place to count
 * what left the string; and both consume ammunition through {@code useAmmo},
 * which makes it the one place Endless Quiver widens what Infinity covers.
 */
@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {
	/** Arrows fired and the tipped checklist, one count per projectile. */
	@Inject(method = "shoot", at = @At("HEAD"))
	private void toolmastery$trackShots(ServerLevel level, LivingEntity shooter, InteractionHand hand,
	                                    ItemStack weapon, List<ItemStack> projectiles, float velocity,
	                                    float inaccuracy, boolean isCrit, LivingEntity target,
	                                    CallbackInfo ci) {
		if (shooter instanceof ServerPlayer player) {
			BowTracker.onArrowsFired(player, weapon, projectiles);
		}
	}

	/**
	 * Endless Quiver: Infinity's ammo rule is asked once, here; answering zero
	 * for a spectral arrow rides vanilla's own no-consume path, intangible
	 * pickup tag included.
	 */
	@Redirect(method = "useAmmo", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"))
	private static int toolmastery$endlessQuiver(ServerLevel level, ItemStack weapon, ItemStack ammo,
	                                             int count, ItemStack weaponArg, ItemStack ammoArg,
	                                             LivingEntity shooter, boolean noConsume) {
		int vanilla = EnchantmentHelper.processAmmoUse(level, weapon, ammo, count);
		return BowPerks.ammoUse(shooter, weapon, ammo, vanilla);
	}
}
