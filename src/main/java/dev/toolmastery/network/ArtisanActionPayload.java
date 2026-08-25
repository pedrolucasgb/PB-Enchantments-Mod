package dev.toolmastery.network;

import dev.toolmastery.ToolMastery;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S: an Artisan action from the inventory or a container screen.
 *
 * <p>Unlike every client-side storage mod, none of this can be done on the
 * client: the tree state lives on the server, so the client only ever expresses
 * intent and the server decides whether the node is unlocked, which containers
 * are in reach and what actually moves.
 *
 * @param action what the player pressed
 * @param slot   inventory slot for {@link Action#TOGGLE_SLOT_LOCK}, else -1
 * @param query  search text for {@link Action#SEARCH}, else empty
 */
public record ArtisanActionPayload(Action action, int slot, String query) implements CustomPacketPayload {
	/** Longest search string the server will look at — a guard, not a feature. */
	public static final int MAX_QUERY = 64;

	public enum Action {
		/** Sorter's Hand I: tidy the player's own backpack. */
		SORT_INVENTORY,
		/** Sorter's Hand II: tidy the container on screen. */
		SORT_CONTAINER,
		/** Artisan's Order: next sort order. */
		CYCLE_SORT_MODE,
		/** Hand of Order: stack everything into the chests that already know it. */
		QUICK_STACK,
		/** Quartermaster's Call: top up what you already carry. */
		RESTOCK,
		/** Seeker's Eye / the Ledger: read-only lookup. */
		SEARCH,
		/** Locked Slots: pin or unpin one inventory slot. */
		TOGGLE_SLOT_LOCK
	}

	public static final Type<ArtisanActionPayload> TYPE =
		new Type<>(Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "artisan_action"));

	public static final StreamCodec<FriendlyByteBuf, ArtisanActionPayload> CODEC =
		CustomPacketPayload.codec(ArtisanActionPayload::write, ArtisanActionPayload::read);

	public static ArtisanActionPayload of(Action action) {
		return new ArtisanActionPayload(action, -1, "");
	}

	public static ArtisanActionPayload search(String query) {
		return new ArtisanActionPayload(Action.SEARCH, -1, query);
	}

	public static ArtisanActionPayload lock(int slot) {
		return new ArtisanActionPayload(Action.TOGGLE_SLOT_LOCK, slot, "");
	}

	private static ArtisanActionPayload read(FriendlyByteBuf buf) {
		int ordinal = buf.readVarInt();
		Action action = ordinal >= 0 && ordinal < Action.values().length
			? Action.values()[ordinal]
			: Action.SEARCH;
		return new ArtisanActionPayload(action, buf.readVarInt(), buf.readUtf(MAX_QUERY));
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(action.ordinal());
		buf.writeVarInt(slot);
		buf.writeUtf(query.length() > MAX_QUERY ? query.substring(0, MAX_QUERY) : query, MAX_QUERY);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
