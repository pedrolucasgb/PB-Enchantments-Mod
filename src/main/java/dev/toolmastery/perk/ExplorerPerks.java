package dev.toolmastery.perk;

import dev.toolmastery.skill.SkillTrees;
import net.minecraft.world.entity.player.Player;

/**
 * Explorer-tree perk lookups shared by the mixins and the tick handlers.
 *
 * <p>Ownership goes through {@link PerkAccess} rather than the attachment, so
 * the two client-side perks — Clear Sight's underwater fog, drawn by the client
 * before the server is ever consulted — get the same answer as the server ones.
 */
public final class ExplorerPerks {
	public static final String CARTOGRAPHER = "cartographer_1";
	public static final String SEA_LEGS = "sea_legs";
	public static final String TIRELESS = "tireless";
	public static final String CLEAR_SIGHT = "clear_sight";
	public static final String NIGHT_EYES = "night_eyes";
	public static final String REMEMBER = "remember";
	public static final String TRAILBLAZER = "trailblazer";
	public static final String SOFT_LANDING = "soft_landing";
	public static final String WAYPOINT = "waypoint";
	public static final String ENDLESS_HORIZON = "endless_horizon";

	private ExplorerPerks() {
	}

	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.EXPLORER, nodeId);
	}

	/** Highest owned rank of a I–III line ("tireless" → tireless_1..3), 0 when none. */
	public static int rank(Player player, String baseId) {
		return PerkAccess.rank(player, SkillTrees.EXPLORER, baseId + "_1", baseId + "_2", baseId + "_3");
	}

	// ---------- Tireless ----------

	/**
	 * What a sprint or a jump costs a player in hunger, as a fraction of
	 * vanilla: 20% off at rank I, 40% at II, 60% at III.
	 *
	 * <p>Only the two exhaustion sources the Explorer earns are scaled — mining,
	 * swimming, taking damage and healing all stay at vanilla rates, because
	 * this class is about covering ground, not about never eating.
	 */
	public static float exhaustionFactor(Player player) {
		return switch (rank(player, TIRELESS)) {
			case 1 -> 0.8F;
			case 2 -> 0.6F;
			case 3 -> 0.4F;
			default -> 1.0F;
		};
	}

	// ---------- Clear Sight ----------

	/**
	 * Extra underwater view distance, as a fraction of the water-vision ramp the
	 * Respiration enchantment already drives. Rank I lands about where
	 * Respiration III does and stacks on top of it; rank II pushes it out to
	 * fully clear water.
	 */
	public static float waterVisionBonus(Player player) {
		return switch (rank(player, CLEAR_SIGHT)) {
			case 1 -> 0.45F;
			case 2 -> 1.0F;
			default -> 0.0F;
		};
	}

	// ---------- Night Eyes ----------

	/**
	 * The strength Night Eyes runs the Night Vision effect at, on the shader's
	 * 0..1 scale where the potion is 1. Strong enough that night is clearly
	 * navigable, weak enough that the real potion is still an upgrade.
	 */
	public static final float NIGHT_EYES_INTENSITY = 0.7F;

	public static boolean seesInTheDark(Player player) {
		return owns(player, NIGHT_EYES);
	}
}
