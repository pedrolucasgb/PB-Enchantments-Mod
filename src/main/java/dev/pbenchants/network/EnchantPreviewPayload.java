package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: the true enchantment lists behind the three enchanting-table offers,
 * already filtered by the player's Arcane Insight level. A slot the player
 * can't read (or with no offer) is an empty list. Sent every time the table's
 * offers change; an all-empty payload clears the client overlay.
 */
public record EnchantPreviewPayload(List<List<Component>> slots) implements CustomPacketPayload {
	public static final int SLOT_COUNT = 3;

	public static final Type<EnchantPreviewPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "enchant_preview"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EnchantPreviewPayload> CODEC =
		CustomPacketPayload.codec(EnchantPreviewPayload::write, EnchantPreviewPayload::read);

	public static EnchantPreviewPayload empty() {
		return new EnchantPreviewPayload(List.of(List.of(), List.of(), List.of()));
	}

	private static EnchantPreviewPayload read(RegistryFriendlyByteBuf buf) {
		List<List<Component>> slots = new ArrayList<>(SLOT_COUNT);
		for (int i = 0; i < SLOT_COUNT; i++) {
			int size = buf.readVarInt();
			List<Component> lines = new ArrayList<>(size);
			for (int j = 0; j < size; j++) {
				lines.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf));
			}
			slots.add(lines);
		}
		return new EnchantPreviewPayload(slots);
	}

	private void write(RegistryFriendlyByteBuf buf) {
		for (int i = 0; i < SLOT_COUNT; i++) {
			List<Component> lines = i < slots.size() ? slots.get(i) : List.of();
			buf.writeVarInt(lines.size());
			for (Component line : lines) {
				ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, line);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
