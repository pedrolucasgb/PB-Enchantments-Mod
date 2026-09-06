package dev.pbenchants.client;

import dev.pbenchants.network.SkillStatePayload;
import dev.pbenchants.perk.AutoBlock;
import dev.pbenchants.storage.SortMode;

/**
 * The client's view of the Artisan tree: which buttons to draw.
 *
 * <p>All of it is advisory. Every button re-checks its node on the server.
 * (Locked items need nothing here: the mark rides on the stack, which the
 * screen already has.)
 */
public final class ClientArtisanState {
	private static final String TREE = "artisan";

	private ClientArtisanState() {
	}

	public static boolean owns(String nodeId) {
		return ClientSkillState.owns(TREE, nodeId);
	}

	/** The sort order the player picked, mirroring {@code StorageTracker.sortMode}. */
	public static SortMode sortMode() {
		SkillStatePayload.TreeState state = ClientSkillState.tree(TREE);
		if (state == null || !state.purchased().contains("sort_profiles")) {
			return SortMode.CATEGORY;
		}
		return SortMode.byIndex(state.counters().getOrDefault("sort_mode", 0));
	}

	public static boolean autoBlockEnabled() {
		SkillStatePayload.TreeState state = ClientSkillState.tree(TREE);

		return state != null
				&& state.purchased().contains(AutoBlock.AUTO_BLOCK)
				&& state.counters().getOrDefault(AutoBlock.AUTO_BLOCK_DISABLED, 0) == 0;
	}
}
