package dev.pbenchants.mixin;

import dev.pbenchants.perk.BoneThrift;
import dev.pbenchants.track.FarmingTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
	/**
	 * At RETURN, so the stack has already been charged — which is exactly what
	 * Bone Thrift refunds.
	 */
	@Inject(method = "useOn", at = @At("RETURN"))
	private void pbenchants$boneMeal(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction() || context.getLevel().isClientSide()) {
			return;
		}
		if (!(context.getPlayer() instanceof ServerPlayer player)) {
			return;
		}
		FarmingTracker.onBoneMeal(player);
		BoneThrift.refund(player, context.getItemInHand());
	}
}
