package dev.toolmastery.storage;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What "the same kind of thing" means to Hand of Order, decided from the item's
 * registry name alone.
 *
 * <p>Quick Stack's safety rule is that an item only ever goes into a container
 * that already knows its kind. Read as "the exact same item" that rule is far
 * too tight: a chest full of oak, birch and spruce planks refuses jungle
 * planks, and a chest of iron and gold ingots refuses copper. The player put
 * those there because they belong together — the button should see it.
 *
 * <p>The obvious fix, item tags, is the wrong tool. Vanilla's item tags mostly
 * describe <em>behaviour</em>, not identity: {@code #wolf_food},
 * {@code #piglin_loved}, {@code #breaks_decorated_pots}. Matching on a shared
 * tag would file a golden apple with gold ingots and every tool with every
 * weapon, and no size or namespace filter separates the identity tags from the
 * behavioural ones.
 *
 * <p>So the rule is the naming convention every registry — vanilla's and every
 * mod's — already follows: <b>the last word of the id is the kind</b>.
 * {@code oak_planks}, {@code birch_planks} and some future mod's
 * {@code ebony_planks} are all "planks"; {@code iron_ingot} and
 * {@code ruby_ingot} are both "ingot"; {@code diamond_sword} and
 * {@code netherite_sword} are both "sword". An id with no underscore is its own
 * kind, which keeps {@code stone} and {@code cobblestone} apart while
 * {@code mossy_cobblestone} still joins the cobblestone.
 *
 * <p>Nothing here is a list of items, so a mod that is not installed yet costs
 * nothing: its items file themselves the moment they exist.
 */
public final class ItemKinship {
	/** Shortest last word that is allowed to name a kind — "_of", "_ii" are not kinds. */
	private static final int MIN_KIND_LENGTH = 3;

	/** Registry ids never change at runtime, so one lookup per item is enough, ever. */
	private static final Map<Item, String> KINDS = new ConcurrentHashMap<>();

	private ItemKinship() {
	}

	/**
	 * The kind this item files under. Two items with equal kinds belong in the
	 * same chest as far as Quick Stack is concerned.
	 */
	public static String kindOf(Item item) {
		return KINDS.computeIfAbsent(item, ItemKinship::compute);
	}

	public static boolean sameKind(Item a, Item b) {
		return a == b || kindOf(a).equals(kindOf(b));
	}

	private static String compute(Item item) {
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		int lastUnderscore = path.lastIndexOf('_');
		if (lastUnderscore < 0 || lastUnderscore == path.length() - 1) {
			return path;
		}
		String kind = path.substring(lastUnderscore + 1);
		// A trailing number or initial ("music_disc_11", "potion_of_x") says
		// nothing about what the item is, so those fall back to the whole id
		// and simply stay their own kind.
		return isWord(kind) ? kind : path;
	}

	private static boolean isWord(String text) {
		if (text.length() < MIN_KIND_LENGTH) {
			return false;
		}
		for (int index = 0; index < text.length(); index++) {
			if (!Character.isLetter(text.charAt(index))) {
				return false;
			}
		}
		return true;
	}
}
