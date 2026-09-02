package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C, configuration phase: "this server runs PB Enchantments {@code version} —
 * what do you have?".
 *
 * <p>Sent while the connection is still being configured, before the player
 * exists in the world. A client that answers with a different version — or that
 * cannot answer at all, because it has no mod to answer with — never gets past
 * the loading screen; see {@link VersionGate}.
 *
 * @param version the server's mod version, e.g. {@code 0.8.1-beta}
 */
public record VersionCheckPayload(String version) implements CustomPacketPayload {
	public static final Type<VersionCheckPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "version_check"));

	public static final StreamCodec<FriendlyByteBuf, VersionCheckPayload> CODEC =
		CustomPacketPayload.codec((payload, buf) -> buf.writeUtf(payload.version()),
			buf -> new VersionCheckPayload(buf.readUtf()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
