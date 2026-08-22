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
 */
public record SkillNode(
	String id,
	int tier,
	int cost,
	@Nullable String requires,
	@Nullable String exclusiveWith,
	SkillType type
) {
	public static SkillNode of(String id, int tier, int cost, SkillType type) {
		return new SkillNode(id, tier, cost, null, null, type);
	}

	public static SkillNode chained(String id, int tier, int cost, String requires, SkillType type) {
		return new SkillNode(id, tier, cost, requires, null, type);
	}

	public static SkillNode capstone(String id, int tier, int cost, String exclusiveWith, SkillType type) {
		return new SkillNode(id, tier, cost, null, exclusiveWith, type);
	}
}
