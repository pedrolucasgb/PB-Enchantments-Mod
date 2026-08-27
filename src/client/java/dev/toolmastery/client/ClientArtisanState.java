package dev.toolmastery.client;

import dev.toolmastery.network.SkillStatePayload;
import dev.toolmastery.storage.SortMode;

/**
 * The client's view of the Artisan tree: which buttons to draw and which slots
 * are pinned.
 *
 * <p>All of it is advisory. Every button re-checks its node on the server, and
 * the pin markers here are only a picture of the long the server sent — the
 * server is the one that refuses to move a pinned slot.
 */
public final class ClientArtisanState {
	private static final String TREE = "artisan";

	private ClientArtisanState() {
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
