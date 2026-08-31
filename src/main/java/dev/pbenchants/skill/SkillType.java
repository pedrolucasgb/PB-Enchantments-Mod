package dev.pbenchants.skill;

/** What kind of unlock a node grants. Drives the GUI badge and application logic. */
public enum SkillType {
	/** Joins the player's enchanting-table pool and can be stamped on a held tool. */
	ENCHANTMENT,
	/** Works the moment it is unlocked; nothing to apply. */
	PASSIVE,
	/** A triggered ability the player fires themselves. */
	ACTIVE,
	/** Unlocks a recipe or grants a custom item. */
	ITEM;

	/** Lower-case id used for lang keys: {@code screen.pbenchants.type.passive}. */
	public String id() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
