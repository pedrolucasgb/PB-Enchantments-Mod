package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: which held ability is currently running, so the client can say so.
 *
 * <p>Sent only on the transition, not every tick — an ability that is on stays on
 * until something says otherwise, and the HUD only needs to be told when that
 * changes. Deliberately off the progress snapshot: this is transient state that
 * dies with the session, and folding it into {@code SkillStatePayload} would make
 * every toggle push the whole tree.
 */
public record AbilityStatePayload(boolean diggyActive) implements CustomPacketPayload {
	public static final Type<AbilityStatePayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "ability_state"));

	public static final StreamCodec<FriendlyByteBuf, AbilityStatePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, AbilityStatePayload::diggyActive,
		AbilityStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
