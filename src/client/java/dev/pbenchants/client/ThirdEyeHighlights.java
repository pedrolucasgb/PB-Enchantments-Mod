package dev.pbenchants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
 * <p><b>The Eye stays open while the lens does.</b> As long as the Seeker's
 * Eye holds a query, the client re-asks the server every couple of seconds —
 * so walking reveals containers as they come into the 3×3×3 reach and, since
 * every answer <em>replaces</em> the marks, the ones you walk away from go
 * dark on the next answer. Clearing the query (or losing the node) sends one
 * empty question, whose empty answer wipes the glow at once; a short expiry
 * remains only as a safety net for a server that stops answering.
 *
 * <p><b>The glow is a silhouette, not a wireframe.</b> Drawing every edge of
 * the {@code VoxelShape} put the chest's back rim across its face and turned
 * a storage hall into scribble. For each axis-aligned box of the shape, an
 * edge is drawn only when exactly one of its two touching faces looks at the
 * camera — the classic silhouette rule — so from any angle you see the
 * block's outline the way your eye would trace it. Still through walls,
 * still the block's own true shape; the drawing is submitted as custom
 * geometry because the stock shape-outline path draws all twelve edges.
 */
public final class ThirdEyeHighlights {
	public static final String NODE = "third_eye";

	/** Gold, like the Seeker's Eye fill the query came from. */
	private static final int COLOR = 0xFFFFD84D;

	/** Safety net only — live answers refresh long before this runs out. */
	private static final int GLOW_TICKS = 80;

	/** How often the open Eye re-asks, in client ticks. */
	private static final int ASK_EVERY = 40;

	private record Mark(BlockPos pos, long expiry) {
	}

	private static final List<Mark> MARKS = new ArrayList<>();

	private static int cadence;

	/** Whether the server currently holds marks for us — what makes one clearing question owed. */
	private static boolean live;

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
		cadence = 0;
		live = false;
	}

	/**
	 * Every client tick: while the lens holds a query, keep asking; the frame
	 * it stops holding one, ask once more with nothing, which is the wipe.
	 */
	public static void clientTick() {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		String query = ClientArtisanState.owns(NODE) ? ArtisanSearch.query().strip() : "";
		if (query.isEmpty()) {
			if (live) {
				live = false;
				ClientPlayNetworking.send(new ThirdEyeQueryPayload(""));
			}
			cadence = 0;
			return;
		}
		if (++cadence >= ASK_EVERY) {
			cadence = 0;
			live = true;
			ClientPlayNetworking.send(new ThirdEyeQueryPayload(query));
		}
	}

	/**
	 * Called when a container screen closes: the first question goes out
	 * immediately rather than waiting for the cadence, so the glow greets you
	 * the moment the screen is gone. Reads the query before
	 * {@link ArtisanSearch#screenClosed()} has a chance to wipe it.
	 */
	public static void queryScreenClosed() {
		String query = ArtisanSearch.query().strip();
		if (query.isEmpty() || !ClientArtisanState.owns(NODE)) {
			return;
		}
		cadence = 0;
		live = true;
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
			// The camera in the block's own [0..1] coordinates — face
			// visibility is decided here, in world space, not on the pose.
			double camX = camera.x - mark.pos().getX();
			double camY = camera.y - mark.pos().getY();
			double camZ = camera.z - mark.pos().getZ();
			VoxelShape finalShape = shape;
			poseStack.pushPose();
			poseStack.translate(
				mark.pos().getX() - camera.x,
				mark.pos().getY() - camera.y,
				mark.pos().getZ() - camera.z);
			collector.submitCustomGeometry(poseStack, ThirdEyeRenderTypes.throughWallLines(),
				(pose, consumer) -> finalShape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
					silhouette(pose, consumer, camX, camY, camZ, x1, y1, z1, x2, y2, z2)));
			poseStack.popPose();
		}
	}

	/**
	 * The twelve edges of one box, kept only where they matter: an edge is
	 * part of the silhouette exactly when one of its two faces is toward the
	 * camera and the other away. A camera level with the box on some axis
	 * sees neither of that axis's faces, and the edges between two hidden
	 * faces vanish with them.
	 */
	private static void silhouette(PoseStack.Pose pose, VertexConsumer consumer,
			double camX, double camY, double camZ,
			double x1, double y1, double z1, double x2, double y2, double z2) {
		boolean xn = camX < x1;
		boolean xp = camX > x2;
		boolean yn = camY < y1;
		boolean yp = camY > y2;
		boolean zn = camZ < z1;
		boolean zp = camZ > z2;

		// Along X: faces Y and Z meet.
		if (yn != zn) {
			line(pose, consumer, x1, y1, z1, x2, y1, z1);
		}
		if (yn != zp) {
			line(pose, consumer, x1, y1, z2, x2, y1, z2);
		}
		if (yp != zn) {
			line(pose, consumer, x1, y2, z1, x2, y2, z1);
		}
		if (yp != zp) {
			line(pose, consumer, x1, y2, z2, x2, y2, z2);
		}
		// Along Y: faces X and Z meet.
		if (xn != zn) {
			line(pose, consumer, x1, y1, z1, x1, y2, z1);
		}
		if (xn != zp) {
			line(pose, consumer, x1, y1, z2, x1, y2, z2);
		}
		if (xp != zn) {
			line(pose, consumer, x2, y1, z1, x2, y2, z1);
		}
		if (xp != zp) {
			line(pose, consumer, x2, y1, z2, x2, y2, z2);
		}
		// Along Z: faces X and Y meet.
		if (xn != yn) {
			line(pose, consumer, x1, y1, z1, x1, y1, z2);
		}
		if (xn != yp) {
			line(pose, consumer, x1, y2, z1, x1, y2, z2);
		}
		if (xp != yn) {
			line(pose, consumer, x2, y1, z1, x2, y1, z2);
		}
		if (xp != yp) {
			line(pose, consumer, x2, y2, z1, x2, y2, z2);
		}
	}

	/** Line width in the 26.2 format is a per-VERTEX element — skip it and the buffer builder refuses the next vertex. */
	private static final float LINE_WIDTH = 2.0F;

	private static void line(PoseStack.Pose pose, VertexConsumer consumer,
			double x1, double y1, double z1, double x2, double y2, double z2) {
		float dx = (float) (x2 - x1);
		float dy = (float) (y2 - y1);
		float dz = (float) (z2 - z1);
		float length = org.joml.Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1.0E-4F) {
			return;
		}
		float nx = dx / length;
		float ny = dy / length;
		float nz = dz / length;
		consumer.addVertex(pose, (float) x1, (float) y1, (float) z1)
			.setColor(COLOR).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
		consumer.addVertex(pose, (float) x2, (float) y2, (float) z2)
			.setColor(COLOR).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
	}
}
