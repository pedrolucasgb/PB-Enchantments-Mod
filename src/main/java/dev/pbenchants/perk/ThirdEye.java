package dev.pbenchants.perk;

import dev.pbenchants.network.ThirdEyeResultPayload;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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
 *
 * <p><b>The reach is a 3×3×3 box of chunk sections around the player</b> —
 * your chunk and its ring, one 16-block layer above and one below — and that
 * ceiling is deliberate (Pedro, 0.10.0 playtest): with a 5×5 full-column scan
 * the Eye read every dungeon chest, buried spawner and neighbour's secret
 * base under your feet. A search that only confirms what is around your own
 * floor finds YOUR chest, not their treasure.
 */
public final class ThirdEye {
	public static final String NODE = "third_eye";

	/** One ring of chunks: a 3×3 centred on the player. */
	private static final int CHUNK_RADIUS = 1;

	/** One 16-block section up and one down from the section your feet are in. */
	private static final int SECTION_RADIUS = 1;

	/** More marks than this is a warehouse, and a warehouse aglow is just noise. */
	private static final int MAX_RESULTS = 128;

	private ThirdEye() {
	}

	public static void handleQuery(ServerPlayer player, String rawQuery) {
		String query = SkillService.owns(player, SkillTrees.ARTISAN, NODE)
			? rawQuery.strip().toLowerCase(Locale.ROOT)
			: "";
		if (query.isEmpty()) {
			// An empty query is the client saying "the lens closed" — answer
			// with an empty set so stale marks vanish at once instead of
			// waiting out their safety timer.
			ServerPlayNetworking.send(player, new ThirdEyeResultPayload(List.of()));
			return;
		}
		List<BlockPos> found = new ArrayList<>();
		ChunkPos centre = player.chunkPosition();
		int playerSection = SectionPos.blockToSectionCoord(player.getBlockY());
		outer:
		for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
			for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
				LevelChunk chunk = player.level().getChunkSource()
					.getChunkNow(centre.x() + dx, centre.z() + dz);
				if (chunk == null) {
					continue;
				}
				for (var entry : chunk.getBlockEntities().entrySet()) {
					// getBlockEntities() hands over the whole column; the
					// height cut is what keeps dungeons under the floor dark.
					int section = SectionPos.blockToSectionCoord(entry.getKey().getY());
					if (Math.abs(section - playerSection) > SECTION_RADIUS) {
						continue;
					}
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
