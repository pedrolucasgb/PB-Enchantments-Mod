package dev.toolmastery.perk;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Side-agnostic "does this player own that node?".
 *
 * <p>Passives that change block-breaking speed have to answer the same on both
 * sides — the client draws the cracking animation, the server re-checks it —
 * but only the server holds the progress attachment. The client installs a
 * lookup over its synced snapshot at init; anywhere else this falls back to
 * the attachment.
 */
public final class PerkAccess {
	/**
	 * Reads the client's synced snapshot. Answers false until the client
	 * installs one, and the client only ever knows about its own player — hence
	 * the {@code player} argument.
	 */
	@FunctionalInterface
	public interface Lookup {
		boolean owns(Player player, String treeId, String nodeId);
	}

	private static Lookup clientLookup = (player, treeId, nodeId) -> false;

	private PerkAccess() {
	}

	public static void setClientLookup(Lookup lookup) {
		clientLookup = lookup;
	}

	public static boolean owns(Player player, SkillTree tree, String nodeId) {
		if (player instanceof ServerPlayer serverPlayer) {
			return SkillService.owns(serverPlayer, tree, nodeId);
		}
		return clientLookup.owns(player, tree.id(), nodeId);
	}

	/**
	 * Highest owned rank of a chained node line, 0 when none is owned.
	 * {@code nodeIds} must be ordered from rank 1 upwards.
	 */
	public static int rank(Player player, SkillTree tree, String... nodeIds) {
		for (int rank = nodeIds.length; rank > 0; rank--) {
			if (owns(player, tree, nodeIds[rank - 1])) {
				return rank;
			}
		}
		return 0;
	}
}
