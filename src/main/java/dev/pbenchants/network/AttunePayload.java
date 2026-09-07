package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: Prism's choice of extra beacon power, pressed on the beacon screen.
 *
 * @param index position in {@code BeaconPerks.ATTUNEMENTS}; 0 is "nothing"
 */
public record AttunePayload(int index) implements CustomPacketPayload {
	public static final Type<AttunePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "attune"));

	public static final StreamCodec<FriendlyByteBuf, AttunePayload> CODEC =
		CustomPacketPayload.codec(AttunePayload::write, AttunePayload::read);

	private static AttunePayload read(FriendlyByteBuf buf) {
		return new AttunePayload(buf.readVarInt());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(index);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
