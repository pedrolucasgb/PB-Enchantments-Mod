package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: the Third Eye asks the world. Sent when an inventory screen closes with
 * a Seeker's Eye query still in the field and the Third Eye node owned — the
 * moment the player stops looking at slots and starts looking at the room.
 * The server re-checks the node and does the searching; the client only ever
 * says what it is looking for.
 */
public record ThirdEyeQueryPayload(String query) implements CustomPacketPayload {
	public static final Type<ThirdEyeQueryPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "third_eye_query"));

	public static final StreamCodec<FriendlyByteBuf, ThirdEyeQueryPayload> CODEC =
		CustomPacketPayload.codec(ThirdEyeQueryPayload::write, ThirdEyeQueryPayload::read);

	private static ThirdEyeQueryPayload read(FriendlyByteBuf buf) {
		return new ThirdEyeQueryPayload(buf.readUtf(64));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeUtf(query, 64);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
