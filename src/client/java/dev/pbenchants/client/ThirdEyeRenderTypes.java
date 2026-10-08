package dev.pbenchants.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * Third Eye's see-through outline: vanilla's {@code lines} render type with the
 * depth test switched off, so a matched block glows through whatever is in
 * front of it. 1.21.1 build — composed from render-state shards (26.x builds
 * it from a render pipeline instead). Created lazily, on the first frame that
 * needs it, never at class init.
 */
public final class ThirdEyeRenderTypes {
	private static RenderType throughWallLines;

	private ThirdEyeRenderTypes() {
	}

	public static RenderType throughWallLines() {
		RenderType type = throughWallLines;
		if (type == null) {
			type = RenderType.create("pbenchants:third_eye_lines",
				DefaultVertexFormat.POSITION_COLOR_NORMAL,
				VertexFormat.Mode.LINES,
				1536,
				false,
				false,
				RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
					.setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
					.setWriteMaskState(RenderStateShard.COLOR_WRITE)
					.setCullState(RenderStateShard.NO_CULL)
					.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
					.createCompositeState(false));
			throughWallLines = type;
		}
		return type;
	}
}
