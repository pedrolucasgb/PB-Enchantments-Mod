package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S2C: full snapshot of the player's skill progress, sent when the client
 * requests it and after every successful action.
 */
public record SkillStatePayload(boolean debugMaster, Map<String, TreeState> trees) implements CustomPacketPayload {
	public record TreeState(int unlockedTiers, Set<String> purchased, Map<String, Integer> counters,
		long lockedSlots) {
	}

	public static final Type<SkillStatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "skill_state"));

	public static final StreamCodec<FriendlyByteBuf, SkillStatePayload> CODEC =
		CustomPacketPayload.codec(SkillStatePayload::write, SkillStatePayload::read);

	private static SkillStatePayload read(FriendlyByteBuf buf) {
		boolean debugMaster = buf.readBoolean();
		int treeCount = buf.readVarInt();
		Map<String, TreeState> trees = new HashMap<>();
		for (int i = 0; i < treeCount; i++) {
			String treeId = buf.readUtf();
			int unlocked = buf.readVarInt();
			int purchasedCount = buf.readVarInt();
			Set<String> purchased = new HashSet<>();
			for (int j = 0; j < purchasedCount; j++) {
				purchased.add(buf.readUtf());
			}
			int counterCount = buf.readVarInt();
			Map<String, Integer> counters = new HashMap<>();
			for (int j = 0; j < counterCount; j++) {
				counters.put(buf.readUtf(), buf.readVarInt());
			}
			trees.put(treeId, new TreeState(unlocked, purchased, counters, buf.readLong()));
		}
		return new SkillStatePayload(debugMaster, trees);
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeBoolean(debugMaster);
		buf.writeVarInt(trees.size());
		for (Map.Entry<String, TreeState> entry : trees.entrySet()) {
			buf.writeUtf(entry.getKey());
			TreeState state = entry.getValue();
			buf.writeVarInt(state.unlockedTiers());
			List<String> purchased = new ArrayList<>(state.purchased());
			buf.writeVarInt(purchased.size());
			for (String nodeId : purchased) {
				buf.writeUtf(nodeId);
			}
			buf.writeVarInt(state.counters().size());
			for (Map.Entry<String, Integer> counter : state.counters().entrySet()) {
				buf.writeUtf(counter.getKey());
				buf.writeVarInt(counter.getValue());
			}
			buf.writeLong(state.lockedSlots());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
