package dev.pbenchants.perk;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Beacon tree's node ids and the small pure rules that are not about the
 * beam itself — what the beam does to a player who stands in it is
 * {@link BeamReceiver}'s.
 *
 * <p>One design rule holds the whole tree together: <b>the beacon never reads
 * anyone's skills.</b> A beacon is a shared block. Every node either changes
 * what the <em>receiving</em> player gets out of a vanilla beacon, or unlocks
 * a craftable thing whose behaviour is the same for everyone. No owner, no
 * keeper, no per-player state on the block entity — which is what keeps the
 * tree safe on a server where six people share one pyramid.
 */
public final class BeaconPerks {
	public static final String SKULL_COLLECTOR = "skull_collector";
	public static final String WITHER_WARD = "wither_ward";
	public static final String BEAM_SENSE = "beam_sense";
	public static final String THRIFTY_OFFERING = "thrifty_offering";
	public static final String RESONANT_HASTE = "resonant_haste";
	public static final String EARLY_REGENERATION = "early_regeneration";
	public static final String BRIGHTER_BEAM = "brighter_beam";
	public static final String STARFALL = "starfall";
	public static final String PHANTOM_TIER = "phantom_tier";

	public static final String[] REACH_OF_THE_BEAM =
		{"reach_of_the_beam_1", "reach_of_the_beam_2", "reach_of_the_beam_3"};
	public static final String[] LINGERING_LIGHT =
		{"lingering_light_1", "lingering_light_2", "lingering_light_3"};
	public static final String[] PRISM = {"prism_1", "prism_2"};

	/**
	 * Skull Collector: vanilla drops a wither skeleton skull 2.5% of the time.
	 * An independent second roll at this chance lands the total at about 6%
	 * ({@code 1 - 0.975 * 0.964}), and stays independent of Looting's own
	 * +1% per level, which vanilla still adds to its roll.
	 */
	public static final float SKULL_COLLECTOR_EXTRA = 0.036F;

	/** Starfall: one Wither in five leaves a second star. */
	public static final float STARFALL_CHANCE = 0.20F;

	/** Thrifty Offering: one payment in four comes back. */
	public static final float THRIFTY_CHANCE = 0.25F;

	/** Wither Ward: what is left of the Wither effect's damage. */
	public static final float WITHER_WARD_REMAINDER = 0.5F;

	/**
	 * The counter Prism keeps its choice in, and what each value means. Index
	 * 0 is "nothing"; the last two need rank II. Stored as a counter because
	 * the tree already syncs those to the client, so a picker in the skill
	 * screen can read it back without a new codec field.
	 */
	public static final String ATTUNE_COUNTER = "attune";

	/** The choices, in counter order; {@code null} at index 0 is "no extra power". */
	public static final List<Holder<MobEffect>> ATTUNEMENTS = java.util.Arrays.asList(
		null,
		MobEffects.NIGHT_VISION,
		MobEffects.FIRE_RESISTANCE,
		MobEffects.SLOW_FALLING,
		MobEffects.SATURATION
	);

	/** The names {@code /pbenchants attune} accepts, in the same order. */
	public static final List<String> ATTUNEMENT_NAMES =
		List.of("none", "night_vision", "fire_resistance", "slow_falling", "saturation");

	/** Rank of Prism needed for each choice: I for the first two, II for the last two. */
	private static final int[] ATTUNEMENT_RANK = {0, 1, 1, 2, 2};

	private BeaconPerks() {
	}

	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.BEACON, nodeId);
	}

	public static int prismRank(Player player) {
		return PerkAccess.rank(player, SkillTrees.BEACON, PRISM);
	}

	/** The rank of Prism a choice needs, by its index in {@link #ATTUNEMENTS}. */
	public static int rankFor(int attunement) {
		return attunement >= 0 && attunement < ATTUNEMENT_RANK.length ? ATTUNEMENT_RANK[attunement] : Integer.MAX_VALUE;
	}

	/** What {@link #attune} has to say: whether the choice took, and the line to tell the player. */
	public record Attuned(boolean ok, net.minecraft.network.chat.Component message) {
	}

	/**
	 * Makes Prism's choice for this player — the beacon screen's row and the
	 * {@code /pbenchants attune} command both land here. Refuses an index off
	 * the list and a choice the player's Prism rank has not opened; the
	 * caller re-syncs the tree state so the screen can show the new frame.
	 */
	public static Attuned attune(ServerPlayer player, int index) {
		if (index < 0 || index >= ATTUNEMENTS.size()) {
			return new Attuned(false, net.minecraft.network.chat.Component.translatable("msg.pbenchants.attune.unknown",
				String.join(", ", ATTUNEMENT_NAMES)));
		}
		int needed = rankFor(index);
		if (prismRank(player) < needed) {
			return new Attuned(false, net.minecraft.network.chat.Component.translatable("msg.pbenchants.attune.locked",
				dev.pbenchants.skill.SkillNode.roman(needed)));
		}
		SkillService.progress(player, SkillTrees.BEACON).counters.put(ATTUNE_COUNTER, index);
		Holder<MobEffect> effect = ATTUNEMENTS.get(index);
		return new Attuned(true, effect == null
			? net.minecraft.network.chat.Component.translatable("msg.pbenchants.attune.none")
			: net.minecraft.network.chat.Component.translatable("msg.pbenchants.attune.set",
				effect.value().getDisplayName()));
	}

	/**
	 * The extra power this player has chosen to receive inside any beacon, or
	 * null. A choice made under a rank the player no longer holds (sold back)
	 * is simply not honoured until they have it again.
	 */
	@Nullable
	public static Holder<MobEffect> attunement(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.BEACON);
		int index = progress.count(ATTUNE_COUNTER);
		if (index <= 0 || index >= ATTUNEMENTS.size() || prismRank(player) < rankFor(index)) {
			return null;
		}
		return ATTUNEMENTS.get(index);
	}
}
