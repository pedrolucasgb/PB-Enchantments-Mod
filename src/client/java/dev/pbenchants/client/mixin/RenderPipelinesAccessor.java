package dev.pbenchants.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reaches vanilla's private LINES snippet so the Third Eye's pipeline stays vanilla in everything but its depth test. */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
	@Accessor("LINES_SNIPPET")
	static RenderPipeline.Snippet pbenchants$linesSnippet() {
		throw new AssertionError();
	}
}
