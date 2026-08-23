package dev.toolmastery.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One material line of a node's unlock cost. Either a concrete item
 * ("32 Coal") or an item tag ("64 Any Log") — the tag form keeps wood-flavoured
 * costs from forcing one specific species on the player.
 *
 * <p>Counting and consumption both run over the whole inventory and are safe on
 * either side: the client uses them to paint the have/need list in the skill
 * screen, the server to actually take the items.
 */
public record MaterialCost(@Nullable Item item, @Nullable TagKey<Item> tag, int count) {
	public MaterialCost {
		if ((item == null) == (tag == null)) {
			throw new IllegalArgumentException("A material cost is exactly one of item or tag");
		}
		if (count <= 0) {
			throw new IllegalArgumentException("Material count must be positive");
		}
	}

	public static MaterialCost of(Item item, int count) {
		return new MaterialCost(item, null, count);
	}

	public static MaterialCost of(TagKey<Item> tag, int count) {
		return new MaterialCost(null, tag, count);
	}

	public boolean matches(ItemStack stack) {
		return item != null ? stack.is(item) : stack.is(tag);
	}

	/** "32 Coal" / "64 Any Log" — the tag label comes from the lang file. */
	public Component label() {
		Component name = item != null
			? Component.translatable(item.getDescriptionId())
			: Component.translatable("material.toolmastery.tag." + tag.location().getPath());
		return Component.literal(count + " ").append(name);
	}

	/** How many of this material the player is carrying (capped at {@link #count}). */
	public int held(Player player) {
		int found = 0;
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (matches(player.getInventory().getItem(slot))) {
				found += player.getInventory().getItem(slot).getCount();
				if (found >= count) {
					return count;
				}
			}
		}
		return found;
	}

	public boolean satisfiedBy(Player player) {
		return held(player) >= count;
	}

	/** The first line the player is short of, or null when everything is covered. */
	@Nullable
	public static MaterialCost missing(Player player, List<MaterialCost> materials) {
		for (MaterialCost material : materials) {
			if (!material.satisfiedBy(player)) {
				return material;
			}
		}
		return null;
	}

	/**
	 * Takes the materials out of the inventory. Callers must have checked
	 * {@link #missing} first — this method assumes the payment is affordable and
	 * consumes whatever it finds.
	 */
	public static void consume(Player player, List<MaterialCost> materials) {
		for (MaterialCost material : materials) {
			int remaining = material.count();
			for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
				ItemStack stack = player.getInventory().getItem(slot);
				if (!material.matches(stack)) {
					continue;
				}
				int taken = Math.min(remaining, stack.getCount());
				stack.shrink(taken);
				remaining -= taken;
			}
		}
	}
}
