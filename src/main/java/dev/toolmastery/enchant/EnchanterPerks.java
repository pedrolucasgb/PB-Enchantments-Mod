package dev.toolmastery.enchant;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Enchanter-tree perk lookups usable from common code on either side.
 * Server players read authoritative progress; on the client the check goes
 * through {@link #clientNodeChecker}, which the client entrypoint wires to the
 * synced skill-state cache. This matters because EnchantmentMenu logic (lapis
 * checks) runs on both sides before the server confirms.
 */
public final class EnchanterPerks {
	/** Node ids — keep in sync with the enchanter tree in SkillTrees. */
	public static final String INNER_FOCUS = "inner_focus";
	public static final String ARCANE_INSIGHT = "arcane_insight";
	public static final String SCHOLAR = "scholar";
	/** Tier 3: anvil work costs 30% fewer levels. */
	public static final String ANVIL_ADEPT = "anvil_adept_1";
	/** Tier 4: no "Too Expensive", and any anvil job caps at {@link #ANVIL_CEILING} levels. */
	public static final String ANVIL_MASTER = "anvil_adept_2";
	/** Tier 5: vanilla Mending may reach level II. */
	public static final String GREATER_MENDING = "greater_mending";
	/** Tier 5: mob XP scales with the Looting on the weapon that landed the kill. */
	public static final String REAPERS_WISDOM = "reapers_wisdom";
	/** Tier 5: the table offers 35/40/45, and sometimes a perfect item. */
	public static final String ANCIENT_KNOWLEDGE = "ancient_knowledge";

	/** What Anvil Adept II charges for a job vanilla would have refused outright. */
	public static final int ANVIL_CEILING = 40;

	/** Levels left of an anvil bill after Anvil Adept I, rounded up. */
	public static final int ANVIL_DISCOUNT_PERCENT = 30;

	/** Set by the client entrypoint; null on a dedicated server. */
	@Nullable
	public static Predicate<String> clientNodeChecker;

	private EnchanterPerks() {
	}

	/** Does this player own the given enchanter node? Works on both sides. */
	public static boolean owns(Player player, String nodeId) {
		if (player instanceof ServerPlayer serverPlayer) {
			return SkillService.owns(serverPlayer, SkillTrees.ENCHANTER, nodeId);
		}
		return clientNodeChecker != null && clientNodeChecker.test(nodeId);
	}

	/**
	 * What an anvil bill of {@code levels} actually costs this player.
	 *
	 * <p>The discount is applied <em>before</em> the ceiling, and before vanilla
	 * decides a job is too expensive: Anvil Adept I on its own can bring a
	 * 55-level merge down under the 40-level wall and make it possible again,
	 * which is most of what the node is for.
	 *
	 * <p>Arithmetic in {@code long} on purpose — vanilla clamps the raw bill at
	 * {@link Integer#MAX_VALUE}, and 70% of that overflows an int.
	 */
	public static int anvilCost(Player player, int levels) {
		if (levels <= 0) {
			return levels;
		}
		long cost = levels;
		if (owns(player, ANVIL_ADEPT)) {
			cost = (cost * (100 - ANVIL_DISCOUNT_PERCENT) + 99) / 100;
		}
		if (owns(player, ANVIL_MASTER)) {
			cost = Math.min(cost, ANVIL_CEILING);
		}
		return (int) Math.min(cost, Integer.MAX_VALUE);
	}

	/** Highest owned level of a ranked node family ("scholar" → scholar_1..3). */
	public static int rankedLevel(Player player, String baseId) {
		int level = 0;
		for (int rank = 1; rank <= 3; rank++) {
			if (owns(player, baseId + "_" + rank)) {
				level = rank;
			}
		}
		return level;
	}
}
