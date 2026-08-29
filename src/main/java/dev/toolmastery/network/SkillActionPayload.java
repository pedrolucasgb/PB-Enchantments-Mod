package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: an action from the skill tree screen. The server validates everything —
 * the client only expresses intent.
 */
public record SkillActionPayload(Action action, String treeId, String nodeId) implements CustomPacketPayload {
	public enum Action {
		REQUEST_STATE,
		UNLOCK_TIER,
		/** One-off purchase: XP points + materials, opens the node. */
		UNLOCK_NODE,
		/** Repeatable: XP points, stamps the enchantment on the held item. */
		ENCHANT_NODE,
		/** Refunds an owned node for a fifth of its unlock price. */
		SELL_NODE,
		/** Repeatable: XP points for an item in the inventory (Biome Chart). */
		BUY_ITEM
	}

	public static final Type<SkillActionPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "skill_action"));

	public static final StreamCodec<FriendlyByteBuf, SkillActionPayload> CODEC =
		CustomPacketPayload.codec(SkillActionPayload::write, SkillActionPayload::read);

	public static SkillActionPayload requestState() {
		return new SkillActionPayload(Action.REQUEST_STATE, "", "");
	}

	private static SkillActionPayload read(FriendlyByteBuf buf) {
		int ordinal = buf.readVarInt();
		Action action = ordinal >= 0 && ordinal < Action.values().length
			? Action.values()[ordinal]
			: Action.REQUEST_STATE;
		return new SkillActionPayload(action, buf.readUtf(), buf.readUtf());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action.ordinal());
		buf.writeUtf(treeId);
		buf.writeUtf(nodeId);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
