package dev.pbenchants.client.mixin;

import dev.pbenchants.perk.ExplorerPerks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.effect.MobEffects;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Night Eyes — a small brightness lift above what the video settings allow.
 *
 * <p>The node shipped unbuilt because 26.2 was read as having deleted the
 * light-texture pipeline. It did not: {@code LightTexture} was <em>renamed</em>
 * to {@link net.minecraft.client.renderer.Lightmap}, and the numbers that feed
 * it were pulled out into {@link LightmapRenderState} — a plain per-frame
 * struct with a {@code brightness} field on it. That field is exactly the dial
 * this perk wanted, and reaching it is better than the old plan ever was:
 *
 * <ul>
 *   <li><b>It is not the player's gamma setting.</b> Every mod that does this
 *       writes to {@code options.gamma()}, which is a persisted preference —
 *       crash mid-session and the player is left with a video setting they
 *       never chose. This is a render-state field rebuilt every frame; nothing
 *       is saved and nothing to restore.</li>
 *   <li><b>It can exceed the slider.</b> The lightmap shader reads brightness
 *       as {@code mix(colour, liftedColour, brightness)} and the lifted colour
 *       equals the original wherever the picture is already bright, so a value
 *       past 1 extrapolates in dark pixels and is a no-op in daylight. That is
 *       the whole node in one line: dark corners lift, noon does not.</li>
 * </ul>
 *
 * <p>The Darkness effect is subtracted afterwards exactly as vanilla does it,
 * so a warden still blinds an Explorer. Ambient light gets a very dark blue
 * floor as well — the lift alone cannot help where the picture is pure black,
 * because anything multiplied by nothing is nothing.
 *
 * <p>Client-only: this is a picture, not a rule, and it reads the synced skill
 * snapshot through {@link ExplorerPerks}, so an unmodded client on a modded
 * server simply sees vanilla darkness.
 */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapExtractorMixin {
	@Inject(method = "extract", at = @At("RETURN"))
	private void pbenchants$nightEyes(LightmapRenderState state, float partialTick, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || !ExplorerPerks.seesInTheDark(player)) {
			return;
		}

		// Recomputed rather than read back off the state: brightness has already
		// had the Darkness effect taken out of it, and raising the floor without
		// re-subtracting would quietly cure a warden's blindness.
		float darknessScale = minecraft.options.darknessEffectScale().get().floatValue();
		float darkness = player.getEffectBlendFactor(MobEffects.DARKNESS, partialTick) * darknessScale;
		float gamma = minecraft.options.gamma().get().floatValue();
		state.brightness = Math.max(0.0F,
			Math.max(gamma, ExplorerPerks.NIGHT_EYES_BRIGHTNESS) - darkness);

		// A floor, never a replacement: a dimension already brighter than this
		// (the Nether) keeps its own ambient light untouched.
		Vector3fc ambient = state.ambientColor;
		state.ambientColor = new Vector3f(
			Math.max(ambient.x(), ExplorerPerks.NIGHT_EYES_AMBIENT_RED),
			Math.max(ambient.y(), ExplorerPerks.NIGHT_EYES_AMBIENT_GREEN),
			Math.max(ambient.z(), ExplorerPerks.NIGHT_EYES_AMBIENT_BLUE));
	}
}
