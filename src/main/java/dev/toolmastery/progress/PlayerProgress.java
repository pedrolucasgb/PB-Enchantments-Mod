package dev.toolmastery.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** All skill-tree progress for one player. Persisted via data attachment. */
public final class PlayerProgress {
	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(Codec.STRING, TreeProgress.CODEC).fieldOf("trees").forGetter(p -> Map.copyOf(p.trees)),
		Codec.BOOL.optionalFieldOf("debug_master", false).forGetter(p -> p.debugMaster)
	).apply(instance, PlayerProgress::new));

	private final Map<String, TreeProgress> trees = new HashMap<>();

	/**
	 * Debug master mode: unlocks are free and skip gates, tiers and materials.
	 * Toggled by {@code /mastery debug master}; persisted so a testing session
	 * survives a relog, and synced so the skill screen shows the free prices.
	 */
	public boolean debugMaster;

	public PlayerProgress() {
	}

	private PlayerProgress(Map<String, TreeProgress> trees, boolean debugMaster) {
		trees.forEach((id, progress) -> this.trees.put(id, progress));
		this.debugMaster = debugMaster;
		migrateShieldBreaker();
	}

	/**
	 * Shield Breaker moved from the Axe tree to the Sword tree in 0.4.0, where a
	 * pure PvP node belongs. A save that already bought it keeps it: the node id
	 * is unchanged, only the tree it hangs in, so the fix is to move the
	 * purchase across rather than to rename anything.
	 *
	 * <p>Cross-tree, so it cannot live in {@link TreeProgress}'s own rename map
	 * — that one only ever sees a single tree.
	 */
	private void migrateShieldBreaker() {
		TreeProgress axe = trees.get("axe");
		if (axe != null && axe.purchased.remove("shield_breaker")) {
			tree("sword").purchased.add("shield_breaker");
		}
	}

	public TreeProgress tree(String treeId) {
		return trees.computeIfAbsent(treeId, ignored -> new TreeProgress());
	}
}
