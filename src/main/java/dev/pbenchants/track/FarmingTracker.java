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

/**
 * Feeds the farming gates of the two trees that farm: the Axe (saplings planted,
 * apples picked up, the sapling checklist) and the Ground tree (crops planted,
 * farmland tilled, bone meal spent). Harvesting itself is counted by
 * {@code BlockBreakTracker}, where the broken block is.
 */
public final class FarmingTracker {
	private FarmingTracker() {
	}

	public static void onSaplingPlaced(ServerPlayer player) {
		SkillService.addCount(player, SkillTrees.AXE, "plant_saplings", 1);
	}

	public static void onApplePickup(ServerPlayer player, int amount) {
		SkillService.addCount(player, SkillTrees.AXE, "harvest_apples", amount);
	}

	/** Ground: a seed, a tuber or a nether wart went into the ground. */
	public static void onCropPlanted(ServerPlayer player) {
		SkillService.addCount(player, SkillTrees.GROUND, "plant_crops", 1);
	}

	public static void onTilled(ServerPlayer player) {
		onTilled(player, 1);
	}

	/** Furrow Hand tills eight extra tiles, and every one of them counts. */
	public static void onTilled(ServerPlayer player, int amount) {
		SkillService.addCount(player, SkillTrees.GROUND, "till_farmland", amount);
	}

	public static void onBoneMeal(ServerPlayer player) {
		SkillService.addCount(player, SkillTrees.GROUND, "bone_meal_used", 1);
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
