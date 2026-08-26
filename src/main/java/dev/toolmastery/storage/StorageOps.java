package dev.toolmastery.storage;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * The Artisan's hands: sorting a container, Quick Stack, and Restock.
 *
 * <p>Everything here runs on the server and takes a {@link ServerPlayer}, even
 * though every mod in the prior art does the same job client-side. It has to:
 * the tree state is the server's, so the client can only ever ask.
 *
 * <p>Two rules are shared by all three operations and are the reason they feel
 * safe rather than clever:
 *
 * <ul>
 *   <li><b>Locked slots are never touched.</b> Pin your pickaxe and no amount
 *       of sorting, stacking or restocking will move it.</li>
 *   <li><b>The hotbar, armour and offhand are never touched</b> by Quick Stack —
 *       what is on your bar is what you chose to have on your bar.</li>
 * </ul>
 */
public final class StorageOps {
	/** First slot of the backpack; 0-8 are the hotbar. */
	public static final int BACKPACK_START = 9;

	/** One past the last backpack slot; 36-39 are armour and 40 the offhand. */
	public static final int BACKPACK_END = 36;

	/** What an operation did, for the action-bar summary. */
	public record Outcome(int items, int containers) {
		public boolean didSomething() {
			return items > 0;
		}
	}

	private StorageOps() {
	}

	// ---------- sorting ----------

	/**
	 * Rearranges a whole container: identical stacks merged, then laid out in
	 * the chosen order from slot 0. Returns true when anything actually moved,
	 * so pressing Sort on an already-sorted chest is a no-op rather than a
	 * counter that ticks for free.
	 */
	public static boolean sortContainer(Container container, SortMode mode) {
		List<ItemStack> contents = new ArrayList<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty()) {
				contents.add(stack.copy());
			}
		}
		List<ItemStack> sorted = mergeAndSort(contents, mode);

		boolean changed = false;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack wanted = slot < sorted.size() ? sorted.get(slot) : ItemStack.EMPTY;
			if (!ItemStack.matches(container.getItem(slot), wanted)) {
				container.setItem(slot, wanted);
				changed = true;
			}
		}
		if (changed) {
			container.setChanged();
		}
		return changed;
	}

	/**
	 * Sorts the player's backpack. The hotbar, armour and offhand are left
	 * alone — a Sort button that reshuffles your hotbar mid-fight is a bug with
	 * a nice icon — and so is every pinned slot, whose contents stay exactly
	 * where they are while everything around them is rearranged.
	 */
	public static boolean sortInventory(ServerPlayer player, SortMode mode) {
		Inventory inventory = player.getInventory();
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);

		List<Integer> slots = new ArrayList<>();
		List<ItemStack> contents = new ArrayList<>();
		for (int slot = BACKPACK_START; slot < BACKPACK_END; slot++) {
			if (progress.slotLocked(slot)) {
				continue;
			}
			slots.add(slot);
			if (!inventory.getItem(slot).isEmpty()) {
				contents.add(inventory.getItem(slot).copy());
			}
		}
		List<ItemStack> sorted = mergeAndSort(contents, mode);

		boolean changed = false;
		for (int index = 0; index < slots.size(); index++) {
			ItemStack wanted = index < sorted.size() ? sorted.get(index) : ItemStack.EMPTY;
			int slot = slots.get(index);
			if (!ItemStack.matches(inventory.getItem(slot), wanted)) {
				inventory.setItem(slot, wanted);
				changed = true;
			}
		}
		return changed;
	}

	/** Merges every stack of the same kind up to its stack limit, then orders the result. */
	private static List<ItemStack> mergeAndSort(List<ItemStack> contents, SortMode mode) {
		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack stack : contents) {
			ItemStack remaining = stack;
			for (ItemStack target : merged) {
				if (remaining.isEmpty()) {
					break;
				}
				if (ItemStack.isSameItemSameComponents(target, remaining)) {
					int room = Math.min(target.getMaxStackSize() - target.getCount(), remaining.getCount());
					if (room > 0) {
						target.grow(room);
						remaining.shrink(room);
					}
				}
			}
			if (!remaining.isEmpty()) {
				merged.add(remaining);
			}
		}
		merged.sort(mode.comparator());
		return merged;
	}

	// ---------- Quick Stack ----------

	/**
	 * Hand of Order: every item you are carrying flies to the nearby chest that
	 * already holds its kind.
	 *
	 * <p>The rule that makes it safe is the "already holds its kind" half. An
	 * item is only ever deposited into a container that already keeps that kind
	 * of thing — Quick Stack joins the organisation you built, it never invents
	 * one, and anything with no home stays in your inventory.
	 *
	 * <p>"Its kind" is deliberately wider than "the identical item": a chest of
	 * oak and birch planks is a planks chest, and jungle planks belong in it.
	 * {@link ItemKinship} decides that, from the item's registry name, so the
	 * rule holds for items this mod has never heard of.
	 *
	 * <p>Two passes, and the order is the whole point: every container that
	 * holds the <em>exact</em> item is offered the stack first, and only what is
	 * left over goes looking for a chest of the same kind. Cobblestone therefore
	 * still lands in the cobblestone chest even when a nearer chest holds stone.
	 *
	 * <p>Within a container, partial stacks are topped up before empty slots are
	 * used, so a chest holding three half-stacks of cobblestone ends with full
	 * stacks rather than five scattered piles. Between containers, the nearest
	 * one that knows the item wins and the next takes the spill.
	 */
	public static Outcome quickStack(ServerPlayer player, int radius) {
		List<ContainerScan.Found> containers = ContainerScan.nearby(player, radius);
		if (containers.isEmpty()) {
			return new Outcome(0, 0);
		}
		List<Set<Item>> exact = new ArrayList<>(containers.size());
		List<Set<String>> kinds = new ArrayList<>(containers.size());
		for (ContainerScan.Found found : containers) {
			Set<Item> types = typesIn(found.container());
			exact.add(types);
			Set<String> kind = new HashSet<>();
			for (Item type : types) {
				kind.add(ItemKinship.kindOf(type));
			}
			kinds.add(kind);
		}

		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		Inventory inventory = player.getInventory();
		Set<Integer> touched = new HashSet<>();
		int moved = 0;

		for (int slot = BACKPACK_START; slot < BACKPACK_END; slot++) {
			if (progress.slotLocked(slot)) {
				continue;
			}
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			moved += pour(stack, containers, touched,
				index -> exact.get(index).contains(stack.getItem()));
			moved += pour(stack, containers, touched,
				index -> kinds.get(index).contains(ItemKinship.kindOf(stack.getItem())));
			if (stack.isEmpty()) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}

		if (moved > 0) {
			progress.addCount("deposit_items", moved);
		}
		return new Outcome(moved, touched.size());
	}

	/**
	 * Offers one stack to every container the predicate accepts, nearest first,
	 * until it is empty. Mutates {@code stack}; returns how many items left it.
	 */
	private static int pour(ItemStack stack, List<ContainerScan.Found> containers, Set<Integer> touched,
			IntPredicate knowsIt) {
		int moved = 0;
		for (int index = 0; index < containers.size() && !stack.isEmpty(); index++) {
			if (!knowsIt.test(index)) {
				continue;
			}
			int before = stack.getCount();
			insert(containers.get(index).container(), stack);
			if (stack.getCount() < before) {
				moved += before - stack.getCount();
				touched.add(index);
			}
		}
		return moved;
	}

	/**
	 * Quartermaster's Call — the mirror of Quick Stack: tops up the stacks you
	 * are already carrying from nearby containers. It only ever refills a type
	 * you already hold, and only into a slot that already holds it, so it never
	 * hands you something new and never fills your last free slot.
	 */
	public static Outcome restock(ServerPlayer player, int radius) {
		List<ContainerScan.Found> containers = ContainerScan.nearby(player, radius);
		if (containers.isEmpty()) {
			return new Outcome(0, 0);
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		Inventory inventory = player.getInventory();
		Set<Integer> touched = new HashSet<>();
		int moved = 0;

		for (int slot = 0; slot < BACKPACK_END; slot++) {
			if (progress.slotLocked(slot)) {
				continue;
			}
			ItemStack held = inventory.getItem(slot);
			if (held.isEmpty() || held.getCount() >= held.getMaxStackSize()) {
				continue;
			}
			for (int index = 0; index < containers.size() && held.getCount() < held.getMaxStackSize(); index++) {
				int taken = withdrawInto(containers.get(index).container(), held);
				if (taken > 0) {
					moved += taken;
					touched.add(index);
				}
			}
		}
		return new Outcome(moved, touched.size());
	}

	/** Every distinct item type in a container — what "already holds its kind" means. */
	private static Set<Item> typesIn(Container container) {
		Set<Item> types = new HashSet<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (!stack.isEmpty()) {
				types.add(stack.getItem());
			}
		}
		return types;
	}

	/** Pours a stack into a container: partial stacks first, then empty slots. Mutates {@code stack}. */
	private static void insert(Container container, ItemStack stack) {
		boolean changed = false;
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			ItemStack target = container.getItem(slot);
			if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, stack)) {
				continue;
			}
			int room = Math.min(target.getMaxStackSize(), container.getMaxStackSize()) - target.getCount();
			int taken = Math.min(room, stack.getCount());
			if (taken > 0) {
				target.grow(taken);
				stack.shrink(taken);
				changed = true;
			}
		}
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack)) {
				continue;
			}
			int taken = Math.min(container.getMaxStackSize(), stack.getCount());
			container.setItem(slot, stack.split(taken));
			changed = true;
		}
		if (changed) {
			container.setChanged();
		}
	}

	/** Tops one held stack up out of a container. Returns how many items moved. */
	private static int withdrawInto(Container container, ItemStack held) {
		int moved = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (held.getCount() >= held.getMaxStackSize()) {
				break;
			}
			ItemStack source = container.getItem(slot);
			if (source.isEmpty() || !ItemStack.isSameItemSameComponents(source, held)) {
				continue;
			}
			int taken = Math.min(held.getMaxStackSize() - held.getCount(), source.getCount());
			held.grow(taken);
			source.shrink(taken);
			if (source.isEmpty()) {
				container.setItem(slot, ItemStack.EMPTY);
			}
			moved += taken;
		}
		if (moved > 0) {
			container.setChanged();
		}
		return moved;
	}
}
