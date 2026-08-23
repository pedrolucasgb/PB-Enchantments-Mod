package dev.toolmastery.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Node ownership seen from either side of the connection.
 *
 * <p>Perks that change block-breaking speed have to run on the client too —
 * vanilla computes the breaking progress there and the server only validates
 * it — but the authoritative progress lives in a server-side attachment. The
 * server answers straight from that attachment; the client mod plugs its
 * synced snapshot in here at init.
 */
public final class NodeOwnership {
	/** Answers for the client's local player; installed by ToolMasteryClient. */
	@FunctionalInterface
	public interface ClientView {
		boolean owns(Player player, String treeId, String nodeId);
	}

	private static ClientView clientView = (player, treeId, nodeId) -> false;

	private NodeOwnership() {
	}

	public static void setClientView(ClientView view) {
		clientView = view;
	}

	/** Does this player own the node? Safe to call from common code on either side. */
	public static boolean owns(Player player, SkillTree tree, String nodeId) {
		if (player instanceof ServerPlayer serverPlayer) {
			return SkillService.owns(serverPlayer, tree, nodeId);
		}
		return clientView.owns(player, tree.id(), nodeId);
	}
}
