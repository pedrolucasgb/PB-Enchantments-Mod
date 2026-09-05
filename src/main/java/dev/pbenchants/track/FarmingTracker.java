package dev.pbenchants.track;

import java.util.HashSet;
import java.util.Set;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dev.pbenchants.skill.GateChecklists;

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
		int mask = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			int bit = saplingBit(stack);

			if (bit >= 0) {
				mask |= 1 << bit;
			}
		}

		TreeProgress progress = SkillService.progress(player, SkillTrees.AXE);
		mask &= GateChecklists.width("sapling_checklist");

		progress.counters.put(
				GateChecklists.maskId("sapling_checklist"),
				mask
		);
		progress.counters.put(
				"sapling_checklist",
				Integer.bitCount(mask)
		);
	}

	private static int saplingBit(ItemStack stack) {
		if (stack.is(Items.OAK_SAPLING)) return 0;
		if (stack.is(Items.SPRUCE_SAPLING)) return 1;
		if (stack.is(Items.BIRCH_SAPLING)) return 2;
		if (stack.is(Items.JUNGLE_SAPLING)) return 3;
		if (stack.is(Items.ACACIA_SAPLING)) return 4;
		if (stack.is(Items.DARK_OAK_SAPLING)) return 5;
		if (stack.is(Items.MANGROVE_PROPAGULE)) return 6;
		if (stack.is(Items.CHERRY_SAPLING)) return 7;
		if (stack.is(Items.PALE_OAK_SAPLING)) return 8;
		if (stack.is(Items.AZALEA)) return 9;
		if (stack.is(Items.FLOWERING_AZALEA)) return 10;
		if (stack.is(Items.CRIMSON_FUNGUS)) return 11;
		if (stack.is(Items.WARPED_FUNGUS)) return 12;
		return -1;
	}
}
