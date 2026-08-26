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
	public static final String WORLDS_MEMORY = "worlds_memory";

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
	 * The brightness Night Eyes holds the lightmap at, on the same 0..1 scale as
	 * the video setting — where 0 is Moody and 1 is the Bright end of the
	 * slider. Above 1 because that is the whole promise of the node: a lift the
	 * options screen cannot give you.
	 *
	 * <p>The shader reads this as {@code mix(colour, liftedColour, brightness)},
	 * and {@code liftedColour} equals {@code colour} wherever the picture is
	 * already bright — so going past 1 extrapolates in dark pixels and does
	 * nothing at all in daylight. Night stays night; the corners stop being a
	 * flat black wall.
	 */
	public static final float NIGHT_EYES_BRIGHTNESS = 1.25F;

	/**
	 * The floor Night Eyes puts under ambient light: a very dark blue, so a
	 * pitch-black cave reads as deep blue rather than as nothing at all.
	 *
	 * <p>Ambient is added to every pixel before the sky and block light, which
	 * is why it has to stay this small — anything higher would wash out daylight
	 * as well, and this node is not a torch.
	 */
	public static final float NIGHT_EYES_AMBIENT_RED = 0.035F;
	public static final float NIGHT_EYES_AMBIENT_GREEN = 0.040F;
	public static final float NIGHT_EYES_AMBIENT_BLUE = 0.060F;

	public static boolean seesInTheDark(Player player) {
		return owns(player, NIGHT_EYES);
	}
}
