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
