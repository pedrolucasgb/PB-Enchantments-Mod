package dev.pbenchants.track;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.storage.SortMode;
import dev.pbenchants.storage.StorageOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Everything that happens around a container the player opened: the
 * {@code deposit_items} gate, and Tidy Storage.
 *
 * <p>Counting deposits from the slot click itself is unreliable — a slot knows
 * what changed but not who changed it, and there are five different put paths.
 * Counting from the <em>menu</em> is exact: snapshot how much is in the
 * container when it opens, compare when it closes, and the difference is what
 * this player put in. Taking things out simply makes the difference negative
 * and is ignored.
 */
public final class StorageTracker {
	/** Item count of the open container, per player, taken when the menu opened. */
	private static final Map<UUID, Integer> opened = new HashMap<>();

	private StorageTracker() {
	}

	public static void onMenuOpened(ServerPlayer player, AbstractContainerMenu menu) {
		opened.put(player.getUUID(), countForeign(player, menu));
	}

	/**
	 * Books the deposit, then tidies up: a container the player has Tidy Storage
	 * for is re-sorted every time they close it, so a chest they once put in
	 * order stays in order however many armfuls get thrown in afterwards.
	 */
	public static void onMenuClosed(ServerPlayer player, AbstractContainerMenu menu) {
		Integer before = opened.remove(player.getUUID());
		if (before == null) {
			return;
		}
		int deposited = countForeign(player, menu) - before;
		if (deposited > 0) {
			SkillService.addCount(player, SkillTrees.ARTISAN, "deposit_items", deposited);
		}

		if (!SkillService.owns(player, SkillTrees.ARTISAN, "tidy_chests")) {
			return;
		}
		Container storage = storageOf(menu);
		if (storage != null && StorageOps.sortContainer(storage, sortMode(player))) {
			SkillService.addCount(player, SkillTrees.ARTISAN, "containers_sorted", 1);
		}
	}

	/** Drops the snapshot when a player leaves mid-chest. */
	public static void forget(ServerPlayer player) {
		opened.remove(player.getUUID());
	}

	/** The sort order this player picked, or the default when Artisan's Order is not unlocked. */
	public static SortMode sortMode(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		if (!progress.owns("sort_profiles")) {
			return SortMode.CATEGORY;
		}
		return SortMode.byIndex(progress.count("sort_mode"));
	}

	/** Cycles the sort order and returns the new one. */
	public static SortMode cycleSortMode(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		SortMode next = SortMode.byIndex(progress.count("sort_mode") + 1);
		progress.counters.put("sort_mode", next.ordinal());
		return next;
	}

	/**
	 * The block storage behind a menu, or null when the menu is not one — Tidy
	 * Storage deliberately never rearranges a furnace or a brewing stand, where
	 * slot position is meaning rather than order.
	 */
	@Nullable
	public static Container storageOf(AbstractContainerMenu menu) {
		if (!(menu instanceof ChestMenu) && !(menu instanceof ShulkerBoxMenu)) {
			return null;
		}
		for (Slot slot : menu.slots) {
			if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)) {
				return slot.container;
			}
		}
		return null;
	}

	/** How many items sit in the non-inventory half of a menu. */
	private static int countForeign(ServerPlayer player, AbstractContainerMenu menu) {
		int total = 0;
		for (Slot slot : menu.slots) {
			if (slot.container == player.getInventory()) {
				continue;
			}
			ItemStack stack = slot.getItem();
			if (!stack.isEmpty()) {
				total += stack.getCount();
			}
		}
		return total;
	}
}
