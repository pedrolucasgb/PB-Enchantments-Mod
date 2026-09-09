package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: the anvil's enchant/disenchant toggle. Carries only the intent; the
 * server re-derives whether a disenchant reading actually exists for the items
 * on the anvil (see {@code AnvilDisenchant.match}), so a stale or hostile
 * toggle can never strip anything a matching book was not paid for.
 */
public record AnvilModePayload(boolean disenchanting) implements CustomPacketPayload {
	public static final Type<AnvilModePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "anvil_mode"));

	public static final StreamCodec<FriendlyByteBuf, AnvilModePayload> CODEC =
		CustomPacketPayload.codec(AnvilModePayload::write, AnvilModePayload::read);

	private static AnvilModePayload read(FriendlyByteBuf buf) {
		return new AnvilModePayload(buf.readBoolean());
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeBoolean(disenchanting);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
