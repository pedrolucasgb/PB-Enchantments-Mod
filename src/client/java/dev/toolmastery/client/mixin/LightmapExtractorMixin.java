package dev.toolmastery.client.mixin;

import dev.toolmastery.perk.ExplorerPerks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night Eyes — a permanent, gentler Night Vision.
 *
 * <p>The first build of this perk nudged {@code brightness} and put a floor
 * under ambient light, and play-testing called the result invisible: the
 * lightmap's brightness dial only stretches what little light there is, so a
 * genuinely dark night still read as black. What players recognise as "seeing
 * in the dark" is the Night Vision <em>effect</em>, and the render state
 * carries exactly that dial: {@code nightVisionEffectIntensity}, the 0..1
 * strength the shader applies the effect at — vanilla writes 1 for the potion.
 *
 * <p>So the perk now floors that intensity at {@link
 * ExplorerPerks#NIGHT_EYES_INTENSITY} (70%): clearly night vision, clearly
 * weaker than the real thing. A floor rather than an override, so drinking an
 * actual potion (or standing in a conduit) still wins with its full 100%, and
 * {@code nightVisionColor} is left alone because vanilla populates it every
 * frame regardless of where the intensity came from. The Darkness effect keeps
 * working exactly as it does against the potion — a warden still blinds you.
 *
 * <p>Client-only: this is a picture, not a rule, and it reads the synced skill
 * snapshot through {@link ExplorerPerks}, so an unmodded client on a modded
 * server simply sees vanilla darkness.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void toolmastery$nightEyes(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !ExplorerPerks.seesInTheDark(player)) {
			return;
		}
		state.nightVisionEffectIntensity =
			Math.max(state.nightVisionEffectIntensity, ExplorerPerks.NIGHT_EYES_INTENSITY);
	}
}
