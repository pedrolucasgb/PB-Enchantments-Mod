package dev.toolmastery.network;

import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.PlayerProgress;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** Wires the C2S action channel and the S2C state channel. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(SkillActionPayload.TYPE, SkillActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillStatePayload.TYPE, SkillStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillFeedbackPayload.TYPE, SkillFeedbackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EnchantPreviewPayload.TYPE, EnchantPreviewPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ArtisanActionPayload.TYPE, ArtisanActionPayload.CODEC);

		// The client needs the snapshot from login on, not from the first time the
		// tree screen is opened: the speed passives are computed client-side while
		// you mine, and the enchanting perks (lapis-free offers, the reroll
		// button) consult it outside the skill screen too.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer()));

		// Artisan buttons live in the inventory screen, not the skill screen, and
		// change the world rather than the tree — so they get their own channel
		// and do not drag a full progress snapshot behind every press. The lock
		// toggle is the exception: it edits the tree, so it re-syncs below.
		ServerPlayNetworking.registerGlobalReceiver(ArtisanActionPayload.TYPE, (payload, context) -> {
			ArtisanHandler.handle(context.player(), payload);
			if (payload.action() == ArtisanActionPayload.Action.TOGGLE_SLOT_LOCK
				|| payload.action() == ArtisanActionPayload.Action.CYCLE_SORT_MODE) {
				sendState(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(SkillActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			switch (payload.action()) {
				case REQUEST_STATE -> {
					// nothing to do — state is sent below
				}
				case UNLOCK_TIER -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					if (tree != null) {
						feedback(player, SkillService.unlockNextTier(player, tree));
					}
				}
				case UNLOCK_NODE -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					SkillNode node = tree == null ? null : tree.node(payload.nodeId());
					if (node != null) {
						feedback(player, SkillService.unlockNode(player, tree, node));
					}
				}
				case ENCHANT_NODE -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					SkillNode node = tree == null ? null : tree.node(payload.nodeId());
					if (node != null) {
						feedback(player, SkillService.enchantHeld(player, tree, node));
					}
				}
				case SELL_NODE -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					SkillNode node = tree == null ? null : tree.node(payload.nodeId());
					if (node != null) {
						feedback(player, SkillService.sellNode(player, tree, node));
					}
				}
				case BUY_ITEM -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					SkillNode node = tree == null ? null : tree.node(payload.nodeId());
					// Only the nodes that actually sell an item honour the action —
					// the client never offers it elsewhere, but the server decides.
					if (node != null && dev.toolmastery.perk.BiomeCharts.isChart(node.id())) {
						feedback(player, dev.toolmastery.perk.BiomeCharts.buy(player, tree, node));
					}
				}
			}
			sendState(player);
		});
	}

	/**
	 * The verdict goes back on its own channel rather than into chat: the
	 * click happened in the skill screen, so the answer belongs there too.
	 * The client decides where to paint it — a banner in the screen while it
	 * is open, chat only if the reply outlived the screen.
	 */
	private static void feedback(ServerPlayer player, SkillService.Result result) {
		switch (result) {
			case SkillService.Result.Ok ok ->
				ServerPlayNetworking.send(player, new SkillFeedbackPayload(true, ok.message()));
			case SkillService.Result.Fail fail ->
				ServerPlayNetworking.send(player, new SkillFeedbackPayload(false, fail.message()));
		}
	}

	/** Pushes the full progress snapshot to one player's client. */
	public static void sendState(ServerPlayer player) {
		PlayerProgress progress = ModAttachments.of(player);
		Map<String, SkillStatePayload.TreeState> trees = new HashMap<>();
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress treeProgress = progress.tree(tree.id());
			trees.put(tree.id(), new SkillStatePayload.TreeState(
				treeProgress.unlockedTiers,
				new HashSet<>(treeProgress.purchased),
				new HashMap<>(treeProgress.counters),
				treeProgress.lockedSlots
			));
		}
		ServerPlayNetworking.send(player, new SkillStatePayload(progress.debugMaster, trees));
	}
}
