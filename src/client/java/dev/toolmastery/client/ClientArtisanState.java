package dev.toolmastery.client;

import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.network.StorageResultPayload;
import dev.toolmastery.storage.SortMode;

import java.util.List;

/**
 * The client's view of the Artisan tree: which buttons to draw, which slots are
 * pinned, and the last answer Seeker's Eye came back with.
 *
 * <p>All of it is advisory. Every button re-checks its node on the server, and
 * the pin markers here are only a picture of the long the server sent — the
 * server is the one that refuses to move a pinned slot.
 */
public final class ClientArtisanState {
	private static final String TREE = "artisan";

	private static List<String> searchResults = List.of();

	private ClientArtisanState() {
	}

	public static void accept(StorageResultPayload payload) {
		searchResults = payload.lines();
	}

	public static List<String> searchResults() {
		return searchResults;
	}

	public static void clearSearch() {
		searchResults = List.of();
	}

	public static boolean owns(String nodeId) {
		return ClientSkillState.owns(TREE, nodeId);
	}

	public static boolean slotLocked(int slot) {
		SkillStatePayload.TreeState state = ClientSkillState.tree(TREE);
		return state != null && slot >= 0 && slot < Long.SIZE
			&& (state.lockedSlots() & (1L << slot)) != 0L;
	}

	/** The sort order the player picked, mirroring {@code StorageTracker.sortMode}. */
	public static SortMode sortMode() {
		SkillStatePayload.TreeState state = ClientSkillState.tree(TREE);
		if (state == null || !state.purchased().contains("sort_profiles")) {
			return SortMode.CATEGORY;
		}
		return SortMode.byIndex(state.counters().getOrDefault("sort_mode", 0));
	}
}
