package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: the answer to a Seeker's Eye search, already formatted.
 *
 * <p>The lines are built server-side because that is where the containers are;
 * sending the raw contents of every chest in a storage room would be a far
 * bigger packet than the sentence the player actually reads.
 */
public record StorageResultPayload(List<String> lines) implements CustomPacketPayload {
	/** Hard cap so a pathological room cannot produce a packet nobody can render. */
	public static final int MAX_LINES = 64;

	public static final Type<StorageResultPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "storage_result"));

	public static final StreamCodec<FriendlyByteBuf, StorageResultPayload> CODEC =
		CustomPacketPayload.codec(StorageResultPayload::write, StorageResultPayload::read);

	private static StorageResultPayload read(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		List<String> lines = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			lines.add(buf.readUtf());
		}
		return new StorageResultPayload(List.copyOf(lines));
	}

	private void write(FriendlyByteBuf buf) {
		int count = Math.min(lines.size(), MAX_LINES);
		buf.writeVarInt(count);
		for (int i = 0; i < count; i++) {
			buf.writeUtf(lines.get(i));
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
