package dev.toolmastery.client;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Seeker's Eye: Ctrl+F for a screen full of slots.
 *
 * <p>You type, and every slot holding a matching item lights up yellow where it
 * already is. Nothing is listed, counted or moved — the answer is the thing you
 * were already looking at.
 *
 * <p>Two ranks, and the difference is reach and memory:
 *
 * <ul>
 *   <li><b>I</b> — your own inventory. The query dies with the screen.</li>
 *   <li><b>II</b> — the open container as well, and the query survives from one
 *       chest to the next, so walking a wall of chests looking for redstone is
 *       one word typed once.</li>
 * </ul>
 *
 * <p>All of it is client-side, and that is not a shortcut: the inventory and the
 * open container are both already on this side of the wire, and highlighting a
 * slot changes nothing a server would need to agree with.
 */
public final class ArtisanSearch {
	/** Longest query the field accepts — a guard, not a feature. */
	public static final int MAX_QUERY = 48;

	private static String query = "";
	private static boolean open;

	private ArtisanSearch() {
	}

	/** Rank of Seeker's Eye the player owns: 0 (none), 1 or 2. */
	public static int rank() {
		if (ClientArtisanState.owns("chest_search_2")) {
			return 2;
		}
		return ClientArtisanState.owns("chest_search_1") ? 1 : 0;
	}

	public static boolean available() {
		return rank() > 0;
	}

	/** True once rank II is in hand: the query is then remembered across screens. */
	public static boolean remembers() {
		return rank() >= 2;
	}

	public static String query() {
		return query;
	}

	public static void setQuery(String text) {
		query = text;
	}

	/** Whether the search field is showing. A non-empty query keeps it up. */
	public static boolean isOpen() {
		return open;
	}

	public static void setOpen(boolean showing) {
		open = showing;
	}

	/**
	 * Called when a screen closes. Rank I forgets what you typed; rank II is
	 * exactly the rank that does not, which is what makes it worth buying.
	 */
	public static void screenClosed() {
		if (!remembers()) {
			query = "";
			open = false;
		}
	}

	/** Wipes everything — on disconnect, and when the node is no longer owned. */
	public static void clear() {
		query = "";
		open = false;
	}

	/**
	 * Whether this slot should light up. Rank I looks only at the player's own
	 * inventory; rank II looks at whatever else is on screen too.
	 */
	public static boolean matches(Slot slot) {
		if (query.isEmpty() || !available()) {
			return false;
		}
		if (rank() < 2 && !(slot.container instanceof Inventory)) {
			return false;
		}
		return matches(slot.getItem());
	}

	/**
	 * Matched against the name on the tooltip rather than the registry id: the
	 * player is searching for what they read in-game, in their own language, and
	 * a renamed stack should answer to its new name.
	 */
	private static boolean matches(ItemStack stack) {
		return !stack.isEmpty()
			&& stack.getHoverName().getString().toLowerCase(Locale.ROOT)
				.contains(query.toLowerCase(Locale.ROOT));
	}
}
