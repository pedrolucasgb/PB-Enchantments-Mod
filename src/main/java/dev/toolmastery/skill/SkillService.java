package dev.toolmastery.skill;

import dev.toolmastery.advancement.ModAdvancements;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.TreeProgress;
import net.minecraft.server.level.ServerPlayer;

/**
 * All progression rules in one place: gate checks, tier unlocking, node purchase.
 * Every method that mutates state also handles the XP payment.
 */
public final class SkillService {
	private SkillService() {
	}

	public static TreeProgress progress(ServerPlayer player, SkillTree tree) {
		return ModAttachments.of(player).tree(tree.id());
	}

	/** True when every gate line of the given tier is complete. */
	public static boolean gateComplete(TreeProgress progress, SkillTier tier) {
		for (GateRequirement gate : tier.gates()) {
			if (progress.count(gate.id()) < gate.target()) {
				return false;
			}
		}
		return true;
	}

	public sealed interface Result {
		record Ok(String message) implements Result {
		}

		record Fail(String message) implements Result {
		}
	}

	/** Attempts to unlock the next tier of a tree. */
	public static Result unlockNextTier(ServerPlayer player, SkillTree tree) {
		TreeProgress progress = progress(player, tree);
		int next = progress.unlockedTiers;
		if (next >= tree.tiers().size()) {
			return new Result.Fail("Every tier of this tree is already unlocked.");
		}
		SkillTier tier = tree.tiers().get(next);
		if (!gateComplete(progress, tier)) {
			return new Result.Fail("Gate incomplete — check /mastery status " + tree.id());
		}
		if (player.experienceLevel < tier.accessCost()) {
			return new Result.Fail("Not enough XP: needs " + tier.accessCost() + " levels, you have " + player.experienceLevel + ".");
		}
		player.giveExperienceLevels(-tier.accessCost());
		progress.unlockedTiers = next + 1;
		ModAdvancements.grantTier(player, tree.id(), next);
		return new Result.Ok("Tier " + (next + 1) + " unlocked for " + tier.accessCost() + " levels!");
	}

	/** Attempts to purchase a node. */
	public static Result buyNode(ServerPlayer player, SkillTree tree, SkillNode node) {
		TreeProgress progress = progress(player, tree);
		if (!node.implemented()) {
			return new Result.Fail("'" + node.id() + "' is coming in a future update and can't be purchased yet.");
		}
		if (progress.owns(node.id())) {
			return new Result.Fail("Already owned.");
		}
		if (node.tier() >= progress.unlockedTiers) {
			return new Result.Fail("Tier " + (node.tier() + 1) + " is still locked.");
		}
		if (node.requires() != null && !progress.owns(node.requires())) {
			return new Result.Fail("Requires '" + node.requires() + "' first.");
		}
		if (node.exclusiveWith() != null && progress.owns(node.exclusiveWith())) {
			return new Result.Fail("Locked by capstone choice '" + node.exclusiveWith() + "'.");
		}
		if (player.experienceLevel < node.cost()) {
			return new Result.Fail("Not enough XP: needs " + node.cost() + " levels, you have " + player.experienceLevel + ".");
		}
		player.giveExperienceLevels(-node.cost());
		progress.purchased.add(node.id());

		String enchantNote = applyGrant(player, node.id());
		return new Result.Ok("'" + node.id() + "' purchased for " + node.cost() + " levels!" + enchantNote);
	}

	/** Applies the node's enchantment grant to the held tool, if any. */
	private static String applyGrant(ServerPlayer player, String nodeId) {
		ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(nodeId);
		if (grant == null) {
			return "";
		}
		if (ModEnchantments.applyToMainHand(player, grant)) {
			return " Enchantment applied to your held tool.";
		}
		return " Unlocked — apply it with /enchant while holding a compatible tool.";
	}

	/** Debug: completes every gate counter and unlocks every tier, free. */
	public static void maxAll(ServerPlayer player) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = progress(player, tree);
			for (SkillTier tier : tree.tiers()) {
				for (GateRequirement gate : tier.gates()) {
					if (progress.count(gate.id()) < gate.target()) {
						progress.counters.put(gate.id(), gate.target());
					}
				}
			}
			progress.unlockedTiers = tree.tiers().size();
		}
		ModAdvancements.syncAll(player);
	}

	/** Debug: grants every node of every tree, free, applying enchant grants to the held tool. */
	public static void buyAll(ServerPlayer player) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = progress(player, tree);
			progress.unlockedTiers = tree.tiers().size();
			for (SkillNode node : tree.nodes().values()) {
				if (!node.implemented()) {
					continue; // same rule as buyNode: nothing to grant yet
				}
				progress.purchased.add(node.id());
				applyGrant(player, node.id());
			}
		}
		ModAdvancements.syncAll(player);
	}

	/** Convenience for tracking hooks: bump a gate counter on the tree. */
	public static void addCount(ServerPlayer player, SkillTree tree, String counterId, int amount) {
		progress(player, tree).addCount(counterId, amount);
	}

	/** Does this player own a node? Cheap enough for event-handler use. */
	public static boolean owns(ServerPlayer player, SkillTree tree, String nodeId) {
		return progress(player, tree).owns(nodeId);
	}

	/**
	 * Highest level of one of our enchantments this player has unlocked in any
	 * tree — the enchanting table offers levels up to this.
	 */
	public static int maxEnchantLevelOwned(ServerPlayer player,
	                                       net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantmentKey) {
		int max = 0;
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = progress(player, tree);
			for (SkillNode node : tree.nodes().values()) {
				ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
				if (grant != null && grant.enchantment() == enchantmentKey && progress.owns(node.id())) {
					max = Math.max(max, grant.level());
				}
			}
		}
		return max;
	}
}
