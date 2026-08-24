package dev.toolmastery.client.mixin;

import dev.toolmastery.perk.ExplorerPerks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clear Sight — underwater fog pushes further out.
 *
 * <p>Vanilla already has exactly the dial this perk wants: {@code waterVision}
 * is the 0..1 ramp the Respiration enchantment drives, and the fog renderer
 * reads it every frame. Adding to it therefore stacks on top of Respiration
 * rather than replacing it, and needs no knowledge of the fog pipeline at all.
 *
 * <p>Client-only by nature — this is a picture, not a rule — and it reads the
 * synced skill snapshot through {@link ExplorerPerks}, so an unmodded client on
 * a modded server simply sees vanilla water.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	@Inject(method = "getWaterVision", at = @At("RETURN"), cancellable = true)
	private void toolmastery$clearSight(CallbackInfoReturnable<Float> cir) {
		float bonus = ExplorerPerks.waterVisionBonus((LocalPlayer) (Object) this);
		if (bonus > 0.0F) {
			cir.setReturnValue(Math.min(1.0F, cir.getReturnValueF() + bonus));
		}
	}
}
