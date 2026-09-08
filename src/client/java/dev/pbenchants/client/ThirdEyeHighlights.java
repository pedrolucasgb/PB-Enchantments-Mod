package dev.pbenchants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pbenchants.network.ThirdEyeQueryPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * The Third Eye's marks: which container blocks are currently aglow, and the
 * drawing of the glow itself.
 *
 * <p>The outline is the block's own {@code VoxelShape} — a hopper glows like a
 * hopper, a chest like a chest — submitted through a lines render type whose
 * depth test always passes, so the shape reads through any amount of stone,
 * exactly the promise a spectral arrow makes about a mob. Ten seconds per
 * answer, refreshed by asking again.
 *
 * <p>Everything here is advisory display state; the server decided what
 * matched. The shape is re-read from the client's own world every frame, so a
 * chest broken mid-glow simply stops having a shape to draw.
 */
public final class ThirdEyeHighlights {
	public static final String NODE = "third_eye";

	/** Gold, like the Seeker's Eye fill the query came from. */
	private static final int COLOR = 0xFFFFD84D;

	private static final int GLOW_TICKS = 200;

	private record Mark(BlockPos pos, long expiry) {
	}

	private static final List<Mark> MARKS = new ArrayList<>();

	private ThirdEyeHighlights() {
	}

	/** The S2C answer: replace, never accumulate — one question, one set of marks. */
	public static void set(List<BlockPos> positions) {
		MARKS.clear();
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		long expiry = level.getGameTime() + GLOW_TICKS;
		for (BlockPos pos : positions) {
			MARKS.add(new Mark(pos, expiry));
		}
	}

	public static void clear() {
		MARKS.clear();
	}

	/**
	 * Called when a container screen closes: with the node owned and a query
	 * still in the Seeker's Eye, the world gets asked. Reads the query before
	 * {@link ArtisanSearch#screenClosed()} has a chance to wipe it.
	 */
	public static void queryScreenClosed() {
		String query = ArtisanSearch.query().strip();
		if (query.isEmpty() || !ClientArtisanState.owns(NODE)) {
			return;
		}
		ClientPlayNetworking.send(new ThirdEyeQueryPayload(query));
	}

	/** Rides the same submit pass as vanilla's own block outline — see LevelRendererMixin. */
	public static void submit(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState renderState) {
		if (MARKS.isEmpty()) {
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			MARKS.clear();
			return;
		}
		long now = level.getGameTime();
		MARKS.removeIf(mark -> now > mark.expiry());
		Vec3 camera = renderState.cameraRenderState.pos;
		for (Mark mark : MARKS) {
			BlockState state = level.getBlockState(mark.pos());
			if (state.isAir()) {
				continue;
			}
			VoxelShape shape = state.getShape(level, mark.pos());
			if (shape.isEmpty()) {
				shape = Shapes.block();
			}
			poseStack.pushPose();
			poseStack.translate(
				mark.pos().getX() - camera.x,
				mark.pos().getY() - camera.y,
				mark.pos().getZ() - camera.z);
			collector.submitShapeOutline(poseStack, shape,
				ThirdEyeRenderTypes.throughWallLines(), COLOR, 2.0F, false);
			poseStack.popPose();
		}
	}
}
