package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: every container block within the Third Eye's reach that holds a match.
 * Positions only — the client draws the outline off its own copy of the block
 * shape, and an empty list is a valid answer that clears the glow.
 */
public record ThirdEyeResultPayload(List<BlockPos> positions) implements CustomPacketPayload {
	public static final Type<ThirdEyeResultPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "third_eye_result"));

	public static final StreamCodec<FriendlyByteBuf, ThirdEyeResultPayload> CODEC =
		CustomPacketPayload.codec(ThirdEyeResultPayload::write, ThirdEyeResultPayload::read);

	private static ThirdEyeResultPayload read(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		List<BlockPos> positions = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			positions.add(buf.readBlockPos());
		}
		return new ThirdEyeResultPayload(positions);
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(positions.size());
		for (BlockPos pos : positions) {
			buf.writeBlockPos(pos);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
