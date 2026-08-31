package dev.pbenchants.client;

import dev.pbenchants.network.EnchantPreviewPayload;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Client-side cache of the Arcane Insight preview — the true enchantment
 * lists behind the enchanting table's three offers, fed by S2C payloads.
 */
public final class EnchantPreviewState {
	private static final List<List<Component>> EMPTY = List.of(List.of(), List.of(), List.of());

	private static List<List<Component>> slots = EMPTY;

	private EnchantPreviewState() {
	}

	public static void accept(EnchantPreviewPayload payload) {
		slots = payload.slots();
	}

	public static void clear() {
		slots = EMPTY;
	}

	public static List<Component> slot(int index) {
		return index >= 0 && index < slots.size() ? slots.get(index) : List.of();
	}

	public static boolean isEmpty() {
		for (List<Component> lines : slots) {
			if (!lines.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
