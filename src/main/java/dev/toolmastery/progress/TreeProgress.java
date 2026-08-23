package dev.toolmastery.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A player's progress inside one skill tree. Mutable; owned by {@link PlayerProgress}. */
public final class TreeProgress {
	public static final Codec<TreeProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("unlocked_tiers").forGetter(t -> t.unlockedTiers),
		Codec.STRING.listOf().fieldOf("purchased").forGetter(t -> List.copyOf(t.purchased)),
		Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("counters").forGetter(t -> Map.copyOf(t.counters))
	).apply(instance, TreeProgress::new));

	/**
	 * Node ids that were renamed after saves already existed, remapped on load
	 * so nobody loses a node to a rename. Melt became Smelt in 0.2.0.
	 */
	private static final Map<String, String> RENAMED = Map.of(
		"melt_1", "smelt_1",
		"melt_2", "smelt_2",
		"melt_3", "smelt_3"
	);

	/** How many tiers are unlocked (0 = none; 1 = tier 1 open, ...). */
	public int unlockedTiers;
	public final Set<String> purchased = new HashSet<>();
	public final Map<String, Integer> counters = new HashMap<>();

	public TreeProgress() {
	}

	private TreeProgress(int unlockedTiers, List<String> purchased, Map<String, Integer> counters) {
		this.unlockedTiers = unlockedTiers;
		for (String nodeId : purchased) {
			this.purchased.add(RENAMED.getOrDefault(nodeId, nodeId));
		}
		this.counters.putAll(counters);
	}

	public int count(String counterId) {
		return counters.getOrDefault(counterId, 0);
	}

	public void addCount(String counterId, int amount) {
		counters.merge(counterId, amount, Integer::sum);
	}

	public boolean owns(String nodeId) {
		return purchased.contains(nodeId);
	}
}
