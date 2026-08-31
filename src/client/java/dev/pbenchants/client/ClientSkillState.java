package dev.pbenchants.client;

import dev.pbenchants.network.SkillStatePayload;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Client-side cache of the player's skill progress, fed by S2C payloads. */
public final class ClientSkillState {
	private static Map<String, SkillStatePayload.TreeState> trees = Map.of();
	@Nullable
	private static Runnable changeListener;

	private ClientSkillState() {
	}

	public static void accept(SkillStatePayload payload) {
		trees = payload.trees();
		if (changeListener != null) {
			changeListener.run();
		}
	}

	/** Drops the cache on disconnect so a stale snapshot never leaks into the next world. */
	public static void clear() {
		trees = Map.of();
	}

	public static boolean owns(String treeId, String nodeId) {
		SkillStatePayload.TreeState state = trees.get(treeId);
		return state != null && state.purchased().contains(nodeId);
	}

	@Nullable
	public static SkillStatePayload.TreeState tree(String treeId) {
		return trees.get(treeId);
	}

	public static void setChangeListener(@Nullable Runnable listener) {
		changeListener = listener;
	}
}
