package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C: the verdict of one skill action — "unlocked", "not enough XP", "no
 * such biome in range" — as the same localised component the chat used to
 * get. The client shows it inside the skill screen when that screen is open,
 * which is where the click that caused it happened; chat is only the fallback
 * for a reply that arrives after the screen closed.
 *
 * <p>The component rides vanilla's own trusted stream codec, so translatable
 * arguments (node names, biome names, point counts) survive the trip and
 * resolve in the client's language.
 */
public record SkillFeedbackPayload(boolean ok, Component message) implements CustomPacketPayload {
	public static final Type<SkillFeedbackPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "skill_feedback"));

	public static final StreamCodec<RegistryFriendlyByteBuf, SkillFeedbackPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, SkillFeedbackPayload::ok,
		ComponentSerialization.TRUSTED_STREAM_CODEC, SkillFeedbackPayload::message,
		SkillFeedbackPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
