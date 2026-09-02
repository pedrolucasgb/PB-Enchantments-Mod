package dev.pbenchants.track;

import com.mojang.serialization.Codec;
import dev.pbenchants.PBEnchants;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;

/**
 * Remembers which log blocks a player placed, so Logic only ever fells grown
 * trees. Vanilla marks player-placed <em>leaves</em> (the persistent property)
 * but stamps nothing on logs, so the mod keeps its own memory — the same
 * answer the tree-feller family of mods settled on ("logs that players place
 * are remembered and never felled").
 *
 * <p>Stored per chunk as a set of packed positions, persisted through the
 * chunk attachment, under the save-facing {@link PBEnchants#DATA_NS}
 * namespace. The record lives exactly as long as the block: marked when a
 * BlockItem placement succeeds, cleared when the log is broken by a player.
 * A log removed some other way (fire, TNT, a piston) can leave a stale entry
 * behind; the only cost is that Logic treats one future block at that exact
 * position as hand-placed, which fails safe.
 *
 * <p>A tree grown from a sapling is not a placement — growth changes blocks
 * without going through {@code BlockItem.place} — so grown trees stay
 * fellable, which is the entire point.
 */
public final class PlacedLogs {
	private static final Codec<LongOpenHashSet> CODEC = Codec.LONG.listOf()
		.xmap(LongOpenHashSet::new, List::copyOf);

	public static final AttachmentType<LongOpenHashSet> PLACED_LOGS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, "placed_logs"),
		builder -> builder
			.initializer(LongOpenHashSet::new)
			.persistent(CODEC)
	);

	private PlacedLogs() {
	}

	/** Called from mod init so the static registration runs at startup. */
	public static void init() {
	}

	/** A player placed a log here. */
	public static void mark(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		LongOpenHashSet placed = chunk.getAttachedOrCreate(PLACED_LOGS);
		if (placed.add(pos.asLong())) {
			chunk.setAttached(PLACED_LOGS, placed); // re-set so the chunk saves
		}
	}

	/** The log here is gone; the record goes with it. */
	public static void clear(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		LongOpenHashSet placed = chunk.getAttached(PLACED_LOGS);
		if (placed != null && placed.remove(pos.asLong())) {
			chunk.setAttached(PLACED_LOGS, placed);
		}
	}

	/** True when the log at this position was placed by hand, not grown. */
	public static boolean isPlaced(ServerLevel level, BlockPos pos) {
		LongOpenHashSet placed = level.getChunkAt(pos).getAttached(PLACED_LOGS);
		return placed != null && placed.contains(pos.asLong());
	}
}
