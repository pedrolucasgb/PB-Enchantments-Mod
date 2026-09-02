package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: "I have an item screen open" / "I closed it".
 *
 * <p>The server cannot work this out on its own. Opening a chest tells it — the
 * menu changes — but opening your own inventory is a purely client-side screen
 * over the menu the player always has, and vanilla sends nothing at all. Deft
 * Hands needs the difference: a hotbar slot going empty means "you used it up"
 * while you are playing and "you are moving things around" while the screen is
 * open, and refilling in the second case is exactly the bug this closes.
 *
 * <p>Sent only when the answer changes, so an idle player costs no traffic.
 *
 * @param open whether an item-handling screen is on the client's display
 */
public record ScreenStatePayload(boolean open) implements CustomPacketPayload {
	public static final Type<ScreenStatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "screen_state"));

	public static final StreamCodec<FriendlyByteBuf, ScreenStatePayload> CODEC =
		CustomPacketPayload.codec((payload, buf) -> buf.writeBoolean(payload.open()),
			buf -> new ScreenStatePayload(buf.readBoolean()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
