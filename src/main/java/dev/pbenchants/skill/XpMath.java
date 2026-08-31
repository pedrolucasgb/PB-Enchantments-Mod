package dev.pbenchants.skill;

import net.minecraft.world.entity.player.Player;

/**
 * The mod's XP economy runs on <b>points</b>, not levels. Levels get more
 * expensive the higher you are — a player at level 50 paying "20 levels" hands
 * over far more experience than one at level 30 paying the same — so charging
 * in levels punishes exactly the players who ground the furthest.
 *
 * <p>Costs are still <em>declared</em> in levels in {@link SkillTrees}, because
 * "about 20 levels" is how a designer thinks. This class converts a declared
 * level cost into its fixed point equivalent: the experience it takes to climb
 * from level 0 to that level, by vanilla's own curve. That number is the same
 * for everyone, whatever level they are standing on.
 */
public final class XpMath {
	private XpMath() {
	}

	/**
	 * Total experience points from level 0 to {@code level} — vanilla's curve,
	 * summed. 5 levels = 55 points, 20 = 550, 30 = 1395, 50 = 5345.
	 */
	public static int pointsForLevel(int level) {
		if (level <= 16) {
			return level * level + 6 * level;
		}
		if (level <= 31) {
			return (int) (2.5 * level * level - 40.5 * level + 360);
		}
		return (int) (4.5 * level * level - 162.5 * level + 2220);
	}

	/**
	 * The points this player is actually carrying, rebuilt from level and bar
	 * progress rather than read from {@code totalExperience} — that field
	 * counts everything ever earned and never goes down when vanilla spends
	 * levels, so it cannot be trusted as a wallet.
	 */
	public static int totalPoints(Player player) {
		return pointsForLevel(player.experienceLevel)
			+ Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
	}
}
