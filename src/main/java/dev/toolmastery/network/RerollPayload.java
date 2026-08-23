package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: the Rewrite Fate capstone's reroll button. Carries no data — the server
 * validates that the player owns the capstone and has an enchanting menu open.
 */
public record RerollPayload() implements CustomPacketPayload {
	public static final Type<RerollPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "reroll"));

	public static final StreamCodec<FriendlyByteBuf, RerollPayload> CODEC =
		StreamCodec.unit(new RerollPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
