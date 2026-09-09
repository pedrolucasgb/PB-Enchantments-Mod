package dev.pbenchants.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pbenchants.client.ThirdEyeHighlights;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Third Eye: the marks ride the same submit pass as vanilla's own crosshair
 * block outline, so they enter the frame exactly where the renderer expects
 * shape outlines and cost nothing when there are none. RETURN rather than
 * TAIL: the method returns early when nothing is under the crosshair, and the
 * Third Eye must glow precisely then — while you wander looking for the chest.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Inject(method = "submitBlockOutline", at = @At("RETURN"))
	private void pbenchants$submitThirdEye(PoseStack poseStack, SubmitNodeCollector collector,
			LevelRenderState renderState, CallbackInfo ci) {
		ThirdEyeHighlights.submit(poseStack, collector, renderState);
	}
}
