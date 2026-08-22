package dev.toolmastery.progress;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

/** All skill-tree progress for one player. Persisted via data attachment. */
public final class PlayerProgress {
	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.unboundedMap(Codec.STRING, TreeProgress.CODEC).fieldOf("trees").forGetter(p -> Map.copyOf(p.trees))
	).apply(instance, PlayerProgress::new));

	private final Map<String, TreeProgress> trees = new HashMap<>();

	public PlayerProgress() {
	}

	private PlayerProgress(Map<String, TreeProgress> trees) {
		trees.forEach((id, progress) -> this.trees.put(id, progress));
	}

	public TreeProgress tree(String treeId) {
		return trees.computeIfAbsent(treeId, ignored -> new TreeProgress());
	}
}
