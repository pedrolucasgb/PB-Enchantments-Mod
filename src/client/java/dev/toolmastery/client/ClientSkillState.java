package dev.toolmastery.client;

import dev.toolmastery.network.SkillStatePayload;
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

	@Nullable
	public static SkillStatePayload.TreeState tree(String treeId) {
		return trees.get(treeId);
	}

	public static void setChangeListener(@Nullable Runnable listener) {
		changeListener = listener;
	}
}
