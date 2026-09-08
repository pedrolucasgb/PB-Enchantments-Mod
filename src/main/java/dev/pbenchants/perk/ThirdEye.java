package dev.pbenchants.perk;

import dev.pbenchants.network.ThirdEyeResultPayload;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Third Eye — the Seeker's Eye, looking through walls (Artisan tier 5).
 *
 * <p>The Seeker's Eye lights up slots on a screen; the Third Eye lights up the
 * <em>blocks</em>. Close the inventory with a query still in the field and
 * every container block within {@link #CHUNK_RADIUS} chunks that holds a match
 * glows through everything for a few seconds, spectral-arrow style — chests,
 * placed shulker boxes, furnaces, hoppers, barrels: anything whose block
 * entity is a {@link Container}.
 *
 * <p>Matching mirrors the Seeker's Eye rule — the name on the tooltip — with
 * one addition: the registry path, so "diamante" finds a renamed stack and
 * "diamond" always finds diamonds even when the server's language is not the
 * player's. Only loaded chunks are asked; the Third Eye sees far, not into
 * places nobody is keeping warm.
 */
public final class ThirdEye {
	public static final String NODE = "third_eye";

	/** "Up to 2 chunks away" — a 5×5 block of chunks centred on the player. */
	private static final int CHUNK_RADIUS = 2;

	/** More marks than this is a warehouse, and a warehouse aglow is just noise. */
	private static final int MAX_RESULTS = 128;

	private ThirdEye() {
	}

	public static void handleQuery(ServerPlayer player, String rawQuery) {
		if (!SkillService.owns(player, SkillTrees.ARTISAN, NODE)) {
			return;
		}
		String query = rawQuery.strip().toLowerCase(Locale.ROOT);
		if (query.isEmpty()) {
			return;
		}
		List<BlockPos> found = new ArrayList<>();
		ChunkPos centre = player.chunkPosition();
		outer:
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				LevelChunk chunk = player.level().getChunkSource()
					.getChunkNow(centre.x() + dx, centre.z() + dz);
				if (chunk == null) {
					continue;
				}
				for (var entry : chunk.getBlockEntities().entrySet()) {
					if (entry.getValue() instanceof Container container && holds(container, query)) {
						found.add(entry.getKey().immutable());
						if (found.size() >= MAX_RESULTS) {
							break outer;
						}
					}
				}
			}
		}
		ServerPlayNetworking.send(player, new ThirdEyeResultPayload(found));
	}

	private static boolean holds(Container container, String query) {
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (matches(container.getItem(slot), query)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(ItemStack stack, String query) {
		if (stack.isEmpty()) {
			return false;
		}
		if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
			return true;
		}
		return stack.getItem().builtInRegistryHolder().key().identifier().getPath()
			.contains(query.replace(' ', '_'));
	}
}
