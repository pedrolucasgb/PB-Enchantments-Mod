package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.client.gui.SkillTreeStyle;
import dev.pbenchants.perk.BeaconPerks;
import dev.pbenchants.skill.SkillNode;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Beam Sense — where the nearest beacon is, and what it is doing to you.
 *
 * <p>Two things vanilla shows nowhere: the powers a beacon has put on you
 * are potion icons in the corner with no timer, and the beacon itself is a
 * beam you can only see with a line of sight. This draws a short column in
 * the top-left: a line pointing at the nearest <em>lit</em> beacon within
 * 128 blocks with its distance, then one line per beacon-granted effect with
 * the seconds it has left — which is also how Lingering Light becomes
 * visible.
 *
 * <p>The beacon search walks the block entities of the loaded chunks around
 * the player once a second; there are at most a few hundred and the map is
 * already in memory, so it costs nothing worth measuring. Client-only: the
 * effects are synced by vanilla and the beacons are rendered by the client
 * anyway, so no packet is needed.
 */
public final class BeamSenseHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "beam_sense");

	/** 128 blocks each way, in chunks. */
	private static final int SCAN_CHUNKS = 8;
	private static final double MAX_DISTANCE_SQ = 128.0 * 128.0;
	private static final long SCAN_INTERVAL_MS = 1000;

	/** Eight compass arrows, clockwise from straight ahead. */
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	private static final int MARGIN = 4;
	private static final int LINE_HEIGHT = 10;

	private static long lastScan;
	@Nullable
	private static BlockPos nearest;

	private BeamSenseHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ID, (graphics, delta) -> draw(graphics));
	}

	public static void clear() {
		nearest = null;
		lastScan = 0;
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null || player.isSpectator()
			|| !ClientSkillState.owns("beacon", BeaconPerks.BEAM_SENSE)) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastScan >= SCAN_INTERVAL_MS) {
			lastScan = now;
			nearest = scan(level, player);
		}

		List<Component> lines = new ArrayList<>();
		if (nearest != null) {
			double dx = nearest.getX() + 0.5 - player.getX();
			double dz = nearest.getZ() + 0.5 - player.getZ();
			int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
			lines.add(Component.translatable("hud.pbenchants.beam_sense.beacon", distance, arrowTowards(player, dx, dz)));
		}
		for (MobEffectInstance effect : player.getActiveEffects()) {
			// A beacon's effects are the ambient ones; a potion's are not.
			if (!effect.isAmbient()) {
				continue;
			}
			String name = effect.getEffect().value().getDisplayName().getString();
			if (effect.getAmplifier() > 0) {
				name += " " + SkillNode.roman(effect.getAmplifier() + 1);
			}
			lines.add(Component.literal(name + "  " + clock(effect)));
		}
		if (lines.isEmpty()) {
			return;
		}
		int y = MARGIN;
		for (int i = 0; i < lines.size(); i++) {
			int color = i == 0 && nearest != null ? SkillTreeStyle.GOLD : SkillTreeStyle.MUTED;
			graphics.text(client.font, lines.get(i), MARGIN, y, color);
			y += LINE_HEIGHT;
		}
	}

	/** The nearest lit beacon in the loaded chunks around the player, or null. */
	@Nullable
	private static BlockPos scan(ClientLevel level, LocalPlayer player) {
		BlockPos origin = player.blockPosition();
		int chunkX = origin.getX() >> 4;
		int chunkZ = origin.getZ() >> 4;
		BlockPos best = null;
		double bestDistance = MAX_DISTANCE_SQ;
		for (int dx = -SCAN_CHUNKS; dx <= SCAN_CHUNKS; dx++) {
			for (int dz = -SCAN_CHUNKS; dz <= SCAN_CHUNKS; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (!(entity instanceof BeaconBlockEntity beacon) || beacon.getBeamSections().isEmpty()) {
						continue;
					}
					double distance = entity.getBlockPos().distSqr(origin);
					if (distance < bestDistance) {
						bestDistance = distance;
						best = entity.getBlockPos();
					}
				}
			}
		}
		return best;
	}

	/**
	 * Which of eight arrows points from where the player is looking towards
	 * the target. Minecraft's yaw is 0 facing south (+z) and grows clockwise
	 * seen from above, so the yaw that faces (dx, dz) is atan2(-dx, dz).
	 */
	private static String arrowTowards(LocalPlayer player, double dx, double dz) {
		double towards = Math.toDegrees(Math.atan2(-dx, dz));
		double relative = Mth.wrapDegrees(towards - player.getYRot());
		int index = (int) Math.floorMod(Math.round(relative / 45.0), 8L);
		return ARROWS[index];
	}

	private static String clock(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return "∞";
		}
		int seconds = effect.getDuration() / 20;
		return seconds >= 60 ? String.format("%d:%02d", seconds / 60, seconds % 60) : seconds + "s";
	}
}
