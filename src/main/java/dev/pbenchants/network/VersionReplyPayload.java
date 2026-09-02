package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S, configuration phase: the client's answer to {@link VersionCheckPayload}.
 *
 * @param version the client's mod version, e.g. {@code 0.8.1-beta}
 */
public record VersionReplyPayload(String version) implements CustomPacketPayload {
	public static final Type<VersionReplyPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "version_reply"));

	public static final StreamCodec<FriendlyByteBuf, VersionReplyPayload> CODEC =
		CustomPacketPayload.codec((payload, buf) -> buf.writeUtf(payload.version()),
			buf -> new VersionReplyPayload(buf.readUtf()));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
