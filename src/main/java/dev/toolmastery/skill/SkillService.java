package dev.toolmastery.skill;

import dev.toolmastery.advancement.ModAdvancements;
import dev.toolmastery.enchant.EnchantCompat;
import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.TreeProgress;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

/**
 * All progression rules in one place: gate checks, tier unlocking, and the two
 * ways a player spends on a node.
 *
 * <p><b>Unlock</b> is the one-off purchase — XP points plus materials. It marks
 * the node owned: a passive starts working immediately, an enchantment joins
 * the player's enchanting-table pool.
 *
 * <p><b>Enchant</b> is the repeatable one — XP points, no materials — and
 * stamps an unlocked enchantment onto whatever the player is holding, after
 * checking that the item and its existing enchantments accept it.
 *
 * <p>All prices are declared in levels but charged in points via
 * {@link XpMath}, so a purchase costs the same experience whatever level the
 * buyer is standing on.
 */
public final class SkillService {
	private SkillService() {
	}

	/**
	 * Debug master mode ({@code /mastery debug master true}): every purchase in
	 * here is free and skips gates, tiers and materials — but the shape of the
	 * tree still holds, so a rank chain is still bought in order. For testing
	 * one node without handing over the whole tree first.
	 */
	public static boolean master(ServerPlayer player) {
		return ModAttachments.of(player).debugMaster;
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
		record Ok(Component message) implements Result {
		}

		record Fail(Component message) implements Result {
		}
	}

	private static Result.Fail fail(String key, Object... args) {
		return new Result.Fail(Component.translatable(key, args));
	}

	private static Result.Ok ok(String key, Object... args) {
		return new Result.Ok(Component.translatable(key, args));
	}

	/** Public factories, for perks that report through the same Result channel. */
	public static Result okFor(String key, Object... args) {
		return ok(key, args);
	}

	public static Result failFor(String key, Object... args) {
		return fail(key, args);
	}

	/** Attempts to unlock the next tier of a tree. */
	public static Result unlockNextTier(ServerPlayer player, SkillTree tree) {
		TreeProgress progress = progress(player, tree);
		int next = progress.unlockedTiers;
		if (next >= tree.tiers().size()) {
			return fail("mastery.toolmastery.tier.fail.complete");
		}
		SkillTier tier = tree.tiers().get(next);
		boolean master = master(player);
		if (!master && !gateComplete(progress, tier)) {
			return fail("mastery.toolmastery.tier.fail.gate", tree.id());
		}
		int cost = master ? 0 : XpMath.pointsForLevel(tier.accessCost());
		if (XpMath.totalPoints(player) < cost) {
			return fail("mastery.toolmastery.fail.xp", cost, XpMath.totalPoints(player));
		}
		if (cost > 0) {
			player.giveExperiencePoints(-cost);
		}
		progress.unlockedTiers = next + 1;
		ModAdvancements.grantTier(player, tree.id(), next);
		return ok("mastery.toolmastery.tier.ok", next + 1, cost);
	}

	/** Attempts to unlock a node: XP points plus the node's materials. */
	public static Result unlockNode(ServerPlayer player, SkillTree tree, SkillNode node) {
		TreeProgress progress = progress(player, tree);
		boolean master = master(player);
		if (!node.implemented()) {
			return fail("mastery.toolmastery.unlock.fail.future", node.displayName());
		}
		if (progress.owns(node.id())) {
			return fail("mastery.toolmastery.unlock.fail.owned", node.displayName());
		}
		if (!master && node.tier() >= progress.unlockedTiers) {
			return fail("mastery.toolmastery.unlock.fail.tier", node.tier() + 1);
		}
		if (node.requires() != null && !progress.owns(node.requires())) {
			return fail("mastery.toolmastery.unlock.fail.requires", SkillNode.displayName(node.requires()));
		}
		String blocker = node.blockedBy(progress::owns);
		if (blocker != null) {
			return fail("mastery.toolmastery.unlock.fail.exclusive", SkillNode.displayName(blocker));
		}
		if (node.requiresAll()) {
			int missing = tree.missingForCompletion(progress.purchased, node.id());
			if (missing > 0) {
				return fail("mastery.toolmastery.unlock.fail.requires_all", missing);
			}
		}
		if (!master) {
			MaterialCost missing = MaterialCost.missing(player, node.materials());
			if (missing != null) {
				return fail("mastery.toolmastery.unlock.fail.materials", missing.label(), missing.held(player));
			}
			int cost = XpMath.pointsForLevel(node.unlockCost());
			if (XpMath.totalPoints(player) < cost) {
				return fail("mastery.toolmastery.fail.xp", cost, XpMath.totalPoints(player));
			}
			MaterialCost.consume(player, node.materials());
			player.giveExperiencePoints(-cost);
		}
		progress.purchased.add(node.id());
		return new Result.Ok(unlockMessage(node));
	}

	/**
	 * Sells an owned node back for one fifth of its unlock price in XP points.
	 * Materials are gone for good, and anything bought on top of this node —
	 * a higher rank, or an everything-first capstone — has to be sold first,
	 * so the tree never holds a node whose prerequisite was refunded away.
	 */
	public static Result sellNode(ServerPlayer player, SkillTree tree, SkillNode node) {
		TreeProgress progress = progress(player, tree);
		if (!progress.owns(node.id())) {
			return fail("mastery.toolmastery.sell.fail.not_owned", node.displayName());
		}
		for (SkillNode other : tree.nodes().values()) {
			if (other.id().equals(node.id()) || !progress.owns(other.id())) {
				continue;
			}
			if (node.id().equals(other.requires()) || other.requiresAll()) {
				return fail("mastery.toolmastery.sell.fail.dependent", other.displayName());
			}
		}
		int refund = XpMath.pointsForLevel(node.unlockCost()) / 5;
		progress.purchased.remove(node.id());
		if (refund > 0) {
			player.giveExperiencePoints(refund);
		}
		return ok("mastery.toolmastery.sell.ok", node.displayName(), refund);
	}

	/**
	 * What an unlock just bought, spelled out — this is the only place the
	 * player is told that the enchantment now shows up at enchanting tables,
	 * and that passives need no second step.
	 */
	private static Component unlockMessage(SkillNode node) {
		if (!node.enchantable()) {
			return Component.translatable(node.type() == SkillType.PASSIVE
				? "mastery.toolmastery.unlock.ok.passive"
				: "mastery.toolmastery.unlock.ok.other", node.displayName());
		}
		ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
		boolean inTable = grant != null && ModEnchantments.TABLE_POOL.contains(grant.enchantment());
		return Component.translatable(inTable
				? "mastery.toolmastery.unlock.ok.enchantment"
				: "mastery.toolmastery.unlock.ok.capstone",
			node.displayName(), node.enchantCost());
	}

	/**
	 * Attempts to stamp an unlocked enchantment onto the player's main-hand
	 * item for the node's enchant price. Repeatable: nothing about the node
	 * changes, only the item.
	 */
	public static Result enchantHeld(ServerPlayer player, SkillTree tree, SkillNode node) {
		if (!node.enchantable()) {
			return fail("mastery.toolmastery.enchant.fail.not_enchantable", node.displayName());
		}
		if (!progress(player, tree).owns(node.id())) {
			return fail("mastery.toolmastery.enchant.fail.locked", node.displayName());
		}
		ModEnchantments.Grant grant = ModEnchantments.NODE_GRANTS.get(node.id());
		Holder<Enchantment> holder = grant == null ? null : ModEnchantments.holder(player, grant.enchantment());
		if (holder == null) {
			return fail("mastery.toolmastery.enchant.fail.not_enchantable", node.displayName());
		}

		ItemStack stack = player.getMainHandItem();
		Component problem = EnchantCompat.problem(stack, holder, grant.level());
		if (problem != null) {
			return new Result.Fail(problem);
		}
		int cost = master(player) ? 0 : XpMath.pointsForLevel(node.enchantCost());
		if (XpMath.totalPoints(player) < cost) {
			return fail("mastery.toolmastery.fail.xp", cost, XpMath.totalPoints(player));
		}

		if (cost > 0) {
			player.giveExperiencePoints(-cost);
		}
		EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, grant.level()));
		return ok("mastery.toolmastery.enchant.ok",
			Enchantment.getFullname(holder, grant.level()), stack.getHoverName(), cost);
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

	/**
	 * Debug: unlocks every node of every tree, free. Enchantments are only
	 * unlocked, not applied — use {@code /mastery debug kit} for tools that
	 * already carry them, or the skill screen's Enchant button.
	 */
	public static void unlockAll(ServerPlayer player) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = progress(player, tree);
			progress.unlockedTiers = tree.tiers().size();
			for (SkillNode node : tree.nodes().values()) {
				if (!node.implemented()) {
					continue; // same rule as unlockNode: nothing to grant yet
				}
				if (node.blockedBy(progress::owns) != null) {
					continue; // a capstone choice stays a choice, even in debug
				}
				progress.purchased.add(node.id());
			}
		}
		ModAdvancements.syncAll(player);
	}

	/**
	 * Debug: wipes a tree back to a brand-new player — no tiers, no nodes, no
	 * gate counters. Pass null to wipe every tree. The exact inverse of
	 * {@link #maxAll} + {@link #unlockAll}, so a feature can be re-tested from
	 * the very first gate.
	 *
	 * <p>Enchantments already stamped on tools are on the items, not in the
	 * progress, so they survive this — {@link #stripHeld} clears those.
	 */
	public static void reset(ServerPlayer player, @Nullable SkillTree only) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			if (only != null && only != tree) {
				continue;
			}
			TreeProgress progress = progress(player, tree);
			progress.unlockedTiers = 0;
			progress.purchased.clear();
			progress.counters.clear();
		}
		ModAdvancements.syncAll(player);
	}

	/**
	 * Debug: hands over one whole tier of a tree — its gates completed, the tier
	 * (and everything below it) open, and every node in it unlocked, free.
	 *
	 * <p>The gap this fills sits between {@code debug tier}, which only moves the
	 * ceiling, and {@code debug unlockall}, which hands over all five tiers of
	 * all three trees: this is "let me look at exactly tier 3 of the pickaxe".
	 *
	 * <p>Nodes that chain off a lower tier drag their prerequisites in with them,
	 * so the tree never ends up showing Smelt II owned without Smelt I. Nodes
	 * that are not implemented yet, and the losing half of a capstone pair, are
	 * skipped and counted.
	 */
	public static Result unlockTierNodes(ServerPlayer player, SkillTree tree, int tierNumber) {
		if (tierNumber < 1 || tierNumber > tree.tiers().size()) {
			return fail("mastery.toolmastery.tier.fail.range_1", tree.tiers().size());
		}
		TreeProgress progress = progress(player, tree);
		int index = tierNumber - 1;

		// Complete the gates up to here too, so /mastery status and the tier
		// headers agree with the tiers this just opened.
		for (int t = 0; t <= index; t++) {
			for (GateRequirement gate : tree.tiers().get(t).gates()) {
				if (progress.count(gate.id()) < gate.target()) {
					progress.counters.put(gate.id(), gate.target());
				}
			}
		}
		progress.unlockedTiers = Math.max(progress.unlockedTiers, tierNumber);

		int granted = 0;
		int skipped = 0;
		for (SkillNode node : tree.nodes().values()) {
			if (node.tier() != index) {
				continue;
			}
			if (!node.implemented() || node.blockedBy(progress::owns) != null) {
				skipped++;
				continue;
			}
			granted += grantWithPrerequisites(tree, progress, node);
		}
		ModAdvancements.syncAll(player);
		return ok("mastery.toolmastery.tier.unlocked", granted, tierNumber, tree.id(), skipped);
	}

	/** Adds a node and everything its {@code requires} chain depends on. Returns how many were new. */
	private static int grantWithPrerequisites(SkillTree tree, TreeProgress progress, SkillNode node) {
		int added = 0;
		for (SkillNode current = node; current != null; current = tree.node(current.requires())) {
			if (progress.purchased.add(current.id())) {
				added++;
			}
			if (current.requires() == null) {
				break;
			}
		}
		return added;
	}

	/**
	 * Debug: re-locks one node, leaving tiers and gate counters alone — for
	 * testing a single unlock over and over without redoing the whole tree.
	 */
	public static Result lockNode(ServerPlayer player, SkillTree tree, SkillNode node) {
		if (!progress(player, tree).purchased.remove(node.id())) {
			return fail("mastery.toolmastery.lock.fail.not_owned", node.displayName());
		}
		return ok("mastery.toolmastery.lock.ok", node.displayName());
	}

	/**
	 * Debug: sets how many tiers of a tree are open, up or down. Nodes above the
	 * new ceiling are re-locked, so lowering the tier is a real rollback rather
	 * than a half-state where a locked tier still has bought nodes.
	 */
	public static Result setTier(ServerPlayer player, SkillTree tree, int tiers) {
		if (tiers < 0 || tiers > tree.tiers().size()) {
			return fail("mastery.toolmastery.tier.fail.range", tree.tiers().size());
		}
		TreeProgress progress = progress(player, tree);
		progress.unlockedTiers = tiers;
		for (SkillNode node : tree.nodes().values()) {
			if (node.tier() >= tiers) {
				progress.purchased.remove(node.id());
			}
		}
		ModAdvancements.syncAll(player);
		return ok("mastery.toolmastery.tier.set", tree.id(), tiers);
	}

	/**
	 * Debug: strips every Tool Mastery enchantment off the held item, so the
	 * same tool can be fed back through the Enchant button.
	 */
	public static Result stripHeld(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (stack.isEmpty()) {
			return fail("mastery.toolmastery.strip.fail.empty");
		}
		int removed = 0;
		for (ModEnchantments.Grant grant : ModEnchantments.NODE_GRANTS.values()) {
			Holder<Enchantment> holder = ModEnchantments.holder(player, grant.enchantment());
			if (holder == null || EnchantmentHelper.getItemEnchantmentLevel(holder, stack) <= 0) {
				continue;
			}
			EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(holder, 0));
			removed++;
		}
		return ok("mastery.toolmastery.strip.ok", removed, stack.getHoverName());
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
	public static int maxEnchantLevelOwned(ServerPlayer player, ResourceKey<Enchantment> enchantmentKey) {
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
