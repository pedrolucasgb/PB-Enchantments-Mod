package dev.toolmastery.network;

import dev.toolmastery.progress.ModAttachments;
import dev.toolmastery.progress.PlayerProgress;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
				case BUY_NODE -> {
					SkillTree tree = SkillTrees.byId(payload.treeId());
					SkillNode node = tree == null ? null : tree.node(payload.nodeId());
					if (node != null) {
						feedback(player, SkillService.buyNode(player, tree, node));
					}
				}
			}
			sendState(player);
		});
	}

	private static void feedback(ServerPlayer player, SkillService.Result result) {
		switch (result) {
			case SkillService.Result.Ok ok ->
				player.sendSystemMessage(Component.literal(ok.message()).withStyle(ChatFormatting.GREEN));
			case SkillService.Result.Fail fail ->
				player.sendSystemMessage(Component.literal(fail.message()).withStyle(ChatFormatting.RED));
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
				new HashMap<>(treeProgress.counters)
			));
		}
		ServerPlayNetworking.send(player, new SkillStatePayload(trees));
	}
}
