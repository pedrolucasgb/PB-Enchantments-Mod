package dev.pbenchants.mixin;

import dev.pbenchants.perk.Slipstream;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Endless Horizon — a quarter of the rockets you burn while flying come back.
 *
 * <p>The redirect sits on the single {@code consume} call inside
 * {@code FireworkRocketItem.use}, which is the elytra-boost path; rockets used
 * on a block ({@code useOn}) go through {@code shrink} instead and are left
 * alone, so the capstone does not quietly refund fireworks at a display.
 */
@Mixin(FireworkRocketItem.class)
public class FireworkRocketItemMixin {
	@Redirect(method = "use", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V"))
	private void pbenchants$endlessHorizonRefund(ItemStack stack, int count, LivingEntity user) {
		if (user instanceof Player player && Slipstream.refunds(player)) {
			return;
		}
		stack.consume(count, user);
	}
}
