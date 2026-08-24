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

	private static final String WOOD_MASK = "overworld_wood_checklist_mask";
	private static final String RARE_MASK = "rare_wood_checklist_mask";
	private static final int OVERWORLD_WOOD_BITS = 0b1_1111_1111;

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
		mergeWoodChecklist();
	}

	/**
	 * A development build briefly cut the Overworld wood checklist in two — six
	 * common woods, three biome-hunt ones. It is one nine-bit line again, read at
	 * two targets, so any save written under the split has its second mask folded
	 * back into the first and the popcount recomputed. A save that never saw the
	 * split has no second mask and falls straight through.
	 */
	private void mergeWoodChecklist() {
		Integer rare = counters.remove(RARE_MASK);
		counters.remove("rare_wood_checklist");
		if (rare == null) {
			return;
		}
		int merged = (counters.getOrDefault(WOOD_MASK, 0) | (rare << 6)) & OVERWORLD_WOOD_BITS;
		counters.put(WOOD_MASK, merged);
		counters.put("overworld_wood_checklist", Integer.bitCount(merged));
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
