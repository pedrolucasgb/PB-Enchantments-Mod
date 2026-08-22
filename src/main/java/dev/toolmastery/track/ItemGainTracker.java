package dev.toolmastery.track;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Feeds gate counters from item acquisition: taking results out of furnace and
 * crafting result slots. Called from the result-slot mixins; onTake fires once
 * per stack moved, so stack counts are per-take amounts.
 */
public final class ItemGainTracker {
	private ItemGainTracker() {
	}

	/** Result taken from any furnace-type menu (furnace, blast furnace, smoker). */
	public static void onSmeltTake(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (stack.is(Items.CHARCOAL)) {
			SkillService.progress(serverPlayer, SkillTrees.AXE).addCount("make_charcoal", stack.getCount());
		}
		if (stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT)
				|| stack.is(Items.GOLD_INGOT) || stack.is(Items.NETHERITE_SCRAP)) {
			SkillService.progress(serverPlayer, SkillTrees.PICKAXE).addCount("smelt_ores", stack.getCount());
		}
	}

	/** Result taken from a crafting grid (table or inventory). */
	public static void onCraftTake(Player player, ItemStack stack) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (stack.is(Items.IRON_PICKAXE)) {
			SkillService.progress(serverPlayer, SkillTrees.PICKAXE).addCount("craft_iron_pickaxe", 1);
		} else if (stack.is(Items.IRON_AXE)) {
			SkillService.progress(serverPlayer, SkillTrees.AXE).addCount("craft_iron_axe", 1);
		}
	}
}
