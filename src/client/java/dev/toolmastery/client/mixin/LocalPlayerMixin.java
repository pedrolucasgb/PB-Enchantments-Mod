package dev.toolmastery.client.mixin;

import dev.toolmastery.perk.BowPerks;
import dev.toolmastery.perk.ExplorerPerks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Three perks that live where the client makes its own decisions.
 *
 * <p><b>Clear Sight</b> (Explorer) — underwater fog pushes further out.
 * {@code waterVision} is the 0..1 ramp the Respiration enchantment drives, and
 * the fog renderer reads it every frame; adding to it stacks on top of
 * Respiration rather than replacing it.
 *
 * <p><b>Swift Draw and Rapid Reload I</b> (Bow) — you keep moving while you
 * aim. Player movement is client-authoritative, and vanilla's slowdown while
 * using an item is a multiplier the input pipeline reads right here — so the
 * multiplier <em>is</em> the rule, no server half needed. The sprint hook is
 * the same fact from the other side: {@code isSlowDueToUsingItem} is what
 * both ends a sprint and forbids starting one, so a crossbow that no longer
 * slows you also no longer breaks your stride.
 *
 * <p>All of it reads the synced skill snapshot, so an unmodded client on a
 * modded server simply moves at vanilla speed.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "getWaterVision", at = @At("RETURN"), cancellable = true)
	private void toolmastery$clearSight(CallbackInfoReturnable<Float> cir) {
		float bonus = ExplorerPerks.waterVisionBonus((LocalPlayer) (Object) this);
		if (bonus > 0.0F) {
			cir.setReturnValue(Math.min(1.0F, cir.getReturnValueF() + bonus));
		}
	}

	/** Swift Draw: 40/60/80% movement while aiming, against vanilla's 20%. */
	@Inject(method = "itemUseSpeedMultiplier", at = @At("RETURN"), cancellable = true)
	private void toolmastery$swiftDraw(CallbackInfoReturnable<Float> cir) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		float lifted = BowPerks.useSpeedMultiplier(self, self.getUseItem(), cir.getReturnValueF());
		if (lifted != cir.getReturnValueF()) {
			cir.setReturnValue(lifted);
		}
	}

	/** Rapid Reload I: a charging crossbow neither ends nor forbids a sprint. */
	@Inject(method = "isSlowDueToUsingItem", at = @At("RETURN"), cancellable = true)
	private void toolmastery$rapidReloadSprint(CallbackInfoReturnable<Boolean> cir) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (cir.getReturnValueZ() && BowPerks.sprintWhileUsing(self, self.getUseItem())) {
			cir.setReturnValue(false);
		}
	}
}
