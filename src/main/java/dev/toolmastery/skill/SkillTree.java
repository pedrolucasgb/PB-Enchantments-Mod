package dev.toolmastery.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A full class tree: ordered tiers plus its purchasable nodes.
 *
 * <p>The {@code icon} rides on the tree so the skill screen's class tab can
 * draw it without a lookup table of its own — adding a class is one entry in
 * {@link SkillTrees#ORDER}.
 */
public final class SkillTree {
	private final String id;
	private final Item icon;
	private final List<SkillTier> tiers;
	private final Map<String, SkillNode> nodes;

	public SkillTree(String id, Item icon, List<SkillTier> tiers, List<SkillNode> nodes) {
		this.id = id;
		this.icon = icon;
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

	/** Class icon for the tab strip. */
	public ItemStack iconStack() {
		return new ItemStack(icon);
	}

	/** "Pickaxe — Path of the Deep". */
	public Component displayName() {
		return Component.translatable("tree.toolmastery." + id);
	}

	/** Just the class half of the name, for the tab label. */
	public Component shortName() {
		return Component.translatable("tree.toolmastery." + id + ".short");
	}

	/** "Stone Apprentice" — the name of a 0-based tier. */
	public Component tierName(int tierIndex) {
		return Component.translatable("tier.toolmastery." + id + "." + (tierIndex + 1));
	}

	public List<SkillTier> tiers() {
		return tiers;
	}

	public Map<String, SkillNode> nodes() {
		return nodes;
	}

	/** The nodes of one tier, in declaration order — the order the GUI stacks them. */
	public List<SkillNode> nodesInTier(int tierIndex) {
		List<SkillNode> inTier = new ArrayList<>();
		for (SkillNode node : nodes.values()) {
			if (node.tier() == tierIndex) {
				inTier.add(node);
			}
		}
		return inTier;
	}

	@Nullable
	public SkillNode node(String nodeId) {
		return nodes.get(nodeId);
	}

	/**
	 * How many nodes of this tree {@code purchased} is still short of owning
	 * everything — the question a {@link SkillNode#requiresAll()} node asks.
	 *
	 * <p>Three kinds do not count against it: the node doing the asking, nodes
	 * that are not built yet, and the losing half of a capstone choice the
	 * player has already made. Without that last exemption a pick-one pair
	 * would make completion impossible by construction.
	 *
	 * <p>Takes a plain set rather than a progress object so the client can ask
	 * it of its synced snapshot and get the same answer as the server.
	 */
	public int missingForCompletion(Set<String> purchased, String askingNodeId) {
		int missing = 0;
		for (SkillNode node : nodes.values()) {
			if (node.id().equals(askingNodeId) || !node.implemented() || purchased.contains(node.id())) {
				continue;
			}
			if (node.blockedBy(purchased::contains) != null) {
				continue; // passed over on purpose, not missing
			}
			missing++;
		}
		return missing;
	}
}
