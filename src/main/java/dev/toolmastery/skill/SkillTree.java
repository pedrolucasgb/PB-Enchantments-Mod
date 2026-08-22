package dev.toolmastery.skill;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A full class tree: ordered tiers plus its purchasable nodes. */
public final class SkillTree {
	private final String id;
	private final List<SkillTier> tiers;
	private final Map<String, SkillNode> nodes;

	public SkillTree(String id, List<SkillTier> tiers, List<SkillNode> nodes) {
		this.id = id;
		this.tiers = List.copyOf(tiers);
		Map<String, SkillNode> byId = new LinkedHashMap<>();
		for (SkillNode node : nodes) {
			if (byId.put(node.id(), node) != null) {
				throw new IllegalArgumentException("Duplicate node id '" + node.id() + "' in tree '" + id + "'");
			}
			if (node.tier() < 0 || node.tier() >= tiers.size()) {
				throw new IllegalArgumentException("Node '" + node.id() + "' references invalid tier " + node.tier());
			}
		}
		this.nodes = byId;
	}

	public String id() {
		return id;
	}

	public List<SkillTier> tiers() {
		return tiers;
	}

	public Map<String, SkillNode> nodes() {
		return nodes;
	}

	@Nullable
	public SkillNode node(String nodeId) {
		return nodes.get(nodeId);
	}
}
