package dev.toolmastery.storage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Locale;

/**
 * How Sorter's Hand arranges a container. Artisan's Order is what lets a player
 * pick one; without that node everybody gets {@link #CATEGORY}.
 */
public enum SortMode {
	/**
	 * The order the game itself lists items in — every plank next to every
	 * other plank, every ore next to every other ore. This is the useful
	 * default, and it is free: the item registry is already in that order.
	 */
	CATEGORY,
	/** Alphabetical by the name on the tooltip, in the player's language. */
	NAME,
	/** Fullest stacks first, so the bulk of a chest is at the top. */
	COUNT;

	public static SortMode byIndex(int index) {
		SortMode[] values = values();
		return values[Math.floorMod(index, values.length)];
	}

	public SortMode next() {
		return byIndex(ordinal() + 1);
	}

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public Component label() {
		return Component.translatable("screen.toolmastery.sort." + id());
	}

	/** Ties always fall through to the same secondary key, so sorting is stable and idempotent. */
	public Comparator<ItemStack> comparator() {
		Comparator<ItemStack> registryOrder =
			Comparator.comparingInt(stack -> BuiltInRegistries.ITEM.getId(stack.getItem()));
		Comparator<ItemStack> byName =
			Comparator.comparing(stack -> stack.getHoverName().getString(), String.CASE_INSENSITIVE_ORDER);
		return switch (this) {
			case CATEGORY -> registryOrder.thenComparing(byName)
				.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
			case NAME -> byName.thenComparing(registryOrder)
				.thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());
			case COUNT -> Comparator.comparingInt(ItemStack::getCount).reversed()
				.thenComparing(registryOrder).thenComparing(byName);
		};
	}
}
