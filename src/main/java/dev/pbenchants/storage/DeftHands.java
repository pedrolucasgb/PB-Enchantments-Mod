package dev.pbenchants.storage;

import dev.pbenchants.network.ScreenStatePayload;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Deft Hands — a hotbar stack that runs out refills itself from your backpack.
 *
 * <p>The trick is knowing <em>what</em> ran out. There is no event for "the
 * stack you were placing just hit zero", so this remembers what each hotbar
 * slot held on the previous tick and reacts when a slot goes from something to
 * nothing.
 *
 * <p>An empty slot is not enough on its own, though, and reading it that way
 * broke shift-click: move a hotbar stack into your backpack and the slot goes
 * empty with the items still in the inventory, so the next tick dutifully
 * pulled the very same stack back out and the click looked like it had done
 * nothing at all. Number-key swaps had the same problem.
 *
 * <p>So the trigger is not "the slot emptied", it is <b>"the slot emptied and
 * there is less of that item in the inventory than there was"</b>. Placing your
 * last dirt block spends it, so the count drops and the slot refills; moving
 * that stack somewhere else inside your own inventory does not change the
 * count, so nothing happens and the item stays where you put it. Emptying the
 * slot into a chest or onto the ground does drop the count, and refilling there
 * is the point of the perk.
 *
 * <p>Pinned slots are never refilled: a locked empty slot is a slot the player
 * deliberately keeps empty.
 *
 * <p>And none of it runs while an item screen is open. Inside the inventory the
 * "less of it than before" rule stops being a good proxy for spending: lift a
 * stack onto the cursor and, as far as the container is concerned, it is gone —
 * so the slot refilled itself under the player's hand and the stack they were
 * carrying had nowhere to go back to. Moving items by hand is not using them
 * up, so the perk stands down for as long as the screen is up. The client says
 * when that is ({@link ScreenStatePayload}); the cursor and the open menu are
 * read here too, so a client that never sends the packet is still safe.
 */
public final class DeftHands {
	/** What each hotbar slot held last tick, and how much of it the player had. */
	private record Memory(Item[] held, int[] carried) {
		static Memory blank() {
			return new Memory(new Item[Inventory.getSelectionSize()], new int[Inventory.getSelectionSize()]);
		}

		void remember(int slot, Item item, int total) {
			held[slot] = item;
			carried[slot] = total;
		}

		void clear(int slot) {
			held[slot] = null;
			carried[slot] = 0;
		}
	}

	private static final Map<UUID, Memory> MEMORIES = new HashMap<>();

	/** Players whose client currently has an item screen open. */
	private static final Set<UUID> BUSY = new HashSet<>();

	private DeftHands() {
	}

	/** The client opened or closed an item screen. */
	public static void setScreenOpen(ServerPlayer player, boolean open) {
		if (open) {
			BUSY.add(player.getUUID());
		} else {
			BUSY.remove(player.getUUID());
		}
	}

	/**
	 * Whether the player is currently handling items by hand rather than
	 * playing. Three independent signs, because no one of them covers
	 * everything: the client's own report (the only thing that sees the
	 * survival inventory), a menu other than the always-open inventory one (a
	 * chest, a crafting table — true even for a client that sends nothing), and
	 * a stack sitting on the cursor mid-drag.
	 */
	private static boolean handlingItems(ServerPlayer player) {
		return BUSY.contains(player.getUUID())
			|| player.containerMenu != player.inventoryMenu
			|| !player.containerMenu.getCarried().isEmpty();
	}

	/** Called every server tick per online player. */
	public static void tick(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		Memory memory = MEMORIES.computeIfAbsent(player.getUUID(), key -> Memory.blank());
		// The memory still keeps up while a screen is open — it is what the
		// slot held and how much of it there was, and both stay true — but
		// nothing refills until the screen is gone.
		boolean enabled = SkillService.owns(player, SkillTrees.ARTISAN, "deft_hands")
			&& !handlingItems(player);
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		Map<Item, Integer> totals = tally(inventory);

		for (int slot = 0; slot < memory.held().length; slot++) {
			ItemStack current = inventory.getItem(slot);
			if (!current.isEmpty()) {
				memory.remember(slot, current.getItem(), count(totals, current.getItem()));
				continue;
			}
			Item ranOut = memory.held()[slot];
			int had = memory.carried()[slot];
			memory.clear(slot);
			if (!enabled || ranOut == null || progress.slotLocked(slot)) {
				continue;
			}
			// Still just as much of it as before: the stack was moved, not spent.
			if (count(totals, ranOut) >= had) {
				continue;
			}
			// A refill only shuffles the item within the inventory, so the tally
			// stays true for the slots after this one.
			if (refill(inventory, progress, slot, ranOut)) {
				memory.remember(slot, ranOut, count(totals, ranOut));
			}
		}
	}

	/** Forgets a player who left, so the maps do not grow forever. */
	public static void forget(ServerPlayer player) {
		MEMORIES.remove(player.getUUID());
		BUSY.remove(player.getUUID());
	}

	/** How much of every item the player is carrying, in one pass. */
	private static Map<Item, Integer> tally(Inventory inventory) {
		Map<Item, Integer> totals = new HashMap<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				totals.merge(stack.getItem(), stack.getCount(), Integer::sum);
			}
		}
		return totals;
	}

	private static int count(Map<Item, Integer> totals, Item item) {
		return totals.getOrDefault(item, 0);
	}

	/** Moves the first matching backpack stack into the empty hotbar slot. */
	private static boolean refill(Inventory inventory, TreeProgress progress, int hotbarSlot, Item wanted) {
		for (int slot = StorageOps.BACKPACK_START; slot < StorageOps.BACKPACK_END; slot++) {
			if (progress.slotLocked(slot)) {
				continue;
			}
			ItemStack candidate = inventory.getItem(slot);
			if (candidate.isEmpty() || !candidate.is(wanted)) {
				continue;
			}
			inventory.setItem(hotbarSlot, candidate);
			inventory.setItem(slot, ItemStack.EMPTY);
			return true;
		}
		return false;
	}
}
