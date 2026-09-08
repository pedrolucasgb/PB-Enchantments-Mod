package dev.pbenchants.client;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.mixin.RenderPipelinesAccessor;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * The one render type the Third Eye needs: vanilla's lines, with the depth
 * test set to always pass — an outline that reads through the wall between
 * you and the chest.
 *
 * <p>Built from vanilla's own {@code LINES_SNIPPET} (reached by accessor —
 * shaders, vertex format and blend all stay exactly vanilla's) with only the
 * {@link DepthStencilState} swapped: {@code ALWAYS_PASS}, no depth write, so
 * the glow neither hides behind terrain nor carves holes into it. Lazy,
 * because a render pipeline may only be built once the GPU device exists.
 */
public final class ThirdEyeRenderTypes {
	private static RenderType throughWallLines;

	private ThirdEyeRenderTypes() {
	}

	public static RenderType throughWallLines() {
		RenderType type = throughWallLines;
		if (type == null) {
			RenderPipeline pipeline = RenderPipeline.builder(RenderPipelinesAccessor.pbenchants$linesSnippet())
				.withLocation(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "pipeline/third_eye_lines"))
				.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
				.build();
			type = RenderType.create("pbenchants:third_eye_lines",
				RenderSetup.builder(pipeline)
					.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
					.setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
					.createRenderSetup());
			throughWallLines = type;
		}
		return type;
	}
}
