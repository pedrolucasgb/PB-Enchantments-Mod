package dev.toolmastery.skill;

import org.jetbrains.annotations.Nullable;

/**
 * A purchasable node in a skill tree.
 *
 * @param id            unique within its tree, snake_case (e.g. "melt_2")
 * @param tier          0-based tier index this node belongs to
 * @param cost          XP levels consumed on purchase
 * @param requires      node id that must be owned first, or null
 * @param exclusiveWith node id that locks this one when owned (capstone choice), or null
 * @param type          what kind of unlock this grants
 * @param implemented   false while the effect ships in a later update — not purchasable yet
 */
public record SkillNode(
	String id,
	int tier,
	int cost,
	@Nullable String requires,
	@Nullable String exclusiveWith,
	SkillType type,
	boolean implemented
) {
	public static SkillNode of(String id, int tier, int cost, SkillType type) {
		return new SkillNode(id, tier, cost, null, null, type, true);
	}

	public static SkillNode chained(String id, int tier, int cost, String requires, SkillType type) {
		return new SkillNode(id, tier, cost, requires, null, type, true);
	}

	public static SkillNode capstone(String id, int tier, int cost, String exclusiveWith, SkillType type) {
		return new SkillNode(id, tier, cost, null, exclusiveWith, type, true);
	}

	/** Marks this node as coming in a future update: visible in the tree but locked. */
	public SkillNode future() {
		return new SkillNode(id, tier, cost, requires, exclusiveWith, type, false);
	}
}
