package dev.toolmastery.storage;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deft Hands — a hotbar stack that runs out refills itself from your backpack.
 *
 * <p>The trick is knowing <em>what</em> ran out. There is no event for "the
 * stack you were placing just hit zero", so this remembers what each hotbar
 * slot held on the previous tick and reacts when a slot goes from something to
 * nothing. That also makes it naturally correct about the difference between
 * placing your last dirt block (refill) and dragging a stack out with the mouse
 * (also a refill, which is fine — you asked for that slot to hold dirt).
 *
 * <p>Pinned slots are never refilled: a locked empty slot is a slot the player
 * deliberately keeps empty.
 */
public final class DeftHands {
	/** What each hotbar slot held last tick, per player. */
	private static final Map<UUID, Item[]> lastHeld = new HashMap<>();

	private DeftHands() {
	}

	/** Called every server tick per online player. */
	public static void tick(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		Item[] previous = lastHeld.computeIfAbsent(player.getUUID(), key -> new Item[Inventory.getSelectionSize()]);
		boolean enabled = SkillService.owns(player, SkillTrees.ARTISAN, "deft_hands");
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);

		for (int slot = 0; slot < previous.length; slot++) {
			ItemStack current = inventory.getItem(slot);
			if (!current.isEmpty()) {
				previous[slot] = current.getItem();
				continue;
			}
			Item ran0ut = previous[slot];
			previous[slot] = null;
			if (!enabled || ran0ut == null || progress.slotLocked(slot)) {
				continue;
			}
			if (refill(inventory, progress, slot, ran0ut)) {
				previous[slot] = ran0ut;
			}
		}
	}

	/** Forgets a player who left, so the map does not grow forever. */
	public static void forget(ServerPlayer player) {
		lastHeld.remove(player.getUUID());
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
