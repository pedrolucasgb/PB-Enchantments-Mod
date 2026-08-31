package dev.pbenchants.client;

import org.jetbrains.annotations.Nullable;

/**
 * The one pinned goal the HUD tracker follows — a single node or a single tier,
 * chosen with the Track button on the skill screen. One at a time by design:
 * the tracker is "what am I working towards right now", not a to-do list.
 *
 * <p>Client-only state. The counters behind it arrive with every progress
 * sync, so the HUD ticks up live as logs are chopped and ores are mined.
 */
public final class GoalTracker {
	/** Either a node ({@code nodeId} set, {@code tier} -1) or a tier ({@code nodeId} null). */
	public record Pin(String treeId, @Nullable String nodeId, int tier) {
	}

	@Nullable
	private static Pin pin;

	private GoalTracker() {
	}

	@Nullable
	public static Pin pinned() {
		return pin;
	}

	public static boolean isPinned(String treeId, @Nullable String nodeId, int tier) {
		return pin != null && pin.equals(new Pin(treeId, nodeId, tier));
	}

	/** Pins this goal, replacing whatever was pinned — or unpins it if it already was. */
	public static void toggle(String treeId, @Nullable String nodeId, int tier) {
		Pin next = new Pin(treeId, nodeId, tier);
		pin = next.equals(pin) ? null : next;
	}

	/** On disconnect: a pin from one world means nothing in the next. */
	public static void clear() {
		pin = null;
	}
}
