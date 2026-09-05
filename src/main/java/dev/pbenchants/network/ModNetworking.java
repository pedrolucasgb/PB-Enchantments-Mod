package dev.pbenchants.network;

import dev.pbenchants.progress.ModAttachments;
import dev.pbenchants.progress.PlayerProgress;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillNode;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.storage.DeftHands;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Wires the C2S action channel and the S2C state channel. */
public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		// Configuration phase: the version handshake runs before the player
		// exists, so a mismatched client never reaches the play payloads below.
		PayloadTypeRegistry.clientboundConfiguration().register(VersionCheckPayload.TYPE, VersionCheckPayload.CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(VersionReplyPayload.TYPE, VersionReplyPayload.CODEC);
		VersionGate.init();

		PayloadTypeRegistry.serverboundPlay().register(SkillActionPayload.TYPE, SkillActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillStatePayload.TYPE, SkillStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SkillFeedbackPayload.TYPE, SkillFeedbackPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EnchantPreviewPayload.TYPE, EnchantPreviewPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AbilityStatePayload.TYPE, AbilityStatePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ArtisanActionPayload.TYPE, ArtisanActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ScreenStatePayload.TYPE, ScreenStatePayload.CODEC);

		// The client needs the snapshot from login on, not from the first time the
		// tree screen is opened: the speed passives are computed client-side while
		// you mine, and the enchanting perks (lapis-free offers, the reroll
		// button) consult it outside the skill screen too.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendState(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			LAST_SYNC.remove(handler.getPlayer().getUUID()));

		// Deft Hands stands down while the player has a screen open, and the
		// survival inventory is the one screen vanilla never tells the server
		// about — so the client says so itself.
		ServerPlayNetworking.registerGlobalReceiver(ScreenStatePayload.TYPE, (payload, context) ->
			DeftHands.setScreenOpen(context.player(), payload.open()));

		// Artisan buttons live in the inventory screen, not the skill screen, and
		// change the world rather than the tree — so they get their own channel
		// and do not drag a full progress snapshot behind every press. The lock
		// toggle is the exception: it edits the tree, so it re-syncs below.
		ServerPlayNetworking.registerGlobalReceiver(ArtisanActionPayload.TYPE, (payload, context) -> {
			ArtisanHandler.handle(context.player(), payload);
			if (payload.action() == ArtisanActionPayload.Action.TOGGLE_SLOT_LOCK
				|| payload.action() == ArtisanActionPayload.Action.CYCLE_SORT_MODE
				|| payload.action() == ArtisanActionPayload.Action.TOGGLE_AUTO_BLOCK) {
				sendState(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(SkillActionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			switch (payload.action()) {
				case REQUEST_STATE -> {
					// The screen asks for a snapshot exactly when it opens, so
					// this is also where the tab's root advancement is earned:
					// the mod's first step is finding the tree at all. State
					// itself is sent below, for every action alike.
					dev.pbenchants.advancement.ModAdvancements.grantRoot(player);
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
					if (node != null && dev.pbenchants.perk.BiomeCharts.isChart(node.id())) {
						feedback(player, dev.pbenchants.perk.BiomeCharts.buy(player, tree, node));
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

	/**
	 * The last snapshot each online player was sent, as a cheap fingerprint.
	 * Gate counters move constantly during play — every log chopped, every
	 * block walked — but for a long time nothing pushed those to the client
	 * outside of login and skill-screen actions, which left the HUD's pinned
	 * goal tracker frozen on a stale snapshot. The slow tick now asks
	 * {@link #syncIfDirty} once a second and only actually sends when the
	 * fingerprint moved, so an idle player costs no traffic at all.
	 */
	private static final Map<UUID, Integer> LAST_SYNC = new HashMap<>();

	/** Called once a second per player: re-sends the snapshot only on change. */
	public static void syncIfDirty(ServerPlayer player) {
		Integer last = LAST_SYNC.get(player.getUUID());
		if (last == null || last != fingerprint(player)) {
			sendState(player);
		}
	}

	private static int fingerprint(ServerPlayer player) {
		PlayerProgress progress = ModAttachments.of(player);
		int hash = Boolean.hashCode(progress.debugMaster);
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress treeProgress = progress.tree(tree.id());
			hash = 31 * hash + treeProgress.unlockedTiers;
			hash = 31 * hash + treeProgress.purchased.hashCode();
			hash = 31 * hash + treeProgress.counters.hashCode();
			hash = 31 * hash + Long.hashCode(treeProgress.lockedSlots);
		}
		return hash;
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
		LAST_SYNC.put(player.getUUID(), fingerprint(player));
		ServerPlayNetworking.send(player, new SkillStatePayload(progress.debugMaster, trees));
	}
}
