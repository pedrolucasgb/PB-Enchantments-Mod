package dev.pbenchants.track;

import java.util.HashSet;
import java.util.Set;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Feeds the farming gates of the axe tree: planting, apple pickups and the sapling checklist. */
public final class FarmingTracker {
	private FarmingTracker() {
	}

	public static void onSaplingPlaced(ServerPlayer player) {
		SkillService.addCount(player, SkillTrees.AXE, "plant_saplings", 1);
	}

	public static void onApplePickup(ServerPlayer player, int amount) {
		SkillService.addCount(player, SkillTrees.AXE, "harvest_apples", amount);
	}

	/**
	 * "All sapling types held at once": once a second, count the distinct
	 * sapling items in the inventory and keep the best result ever reached.
	 */
	public static void scanSaplingChecklist(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		Set<Item> types = new HashSet<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.is(ItemTags.SAPLINGS)) {
				types.add(stack.getItem());
			}
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.AXE);
		if (types.size() > progress.count("sapling_checklist")) {
			progress.counters.put("sapling_checklist", types.size());
		}
	}
}
