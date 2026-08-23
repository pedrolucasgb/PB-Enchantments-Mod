package dev.toolmastery.track;

import dev.toolmastery.ToolMastery;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Feeds gate counters from item acquisition: taking results out of furnace and
 * crafting result slots. Called from the result-slot mixins with the amount
 * actually removed (vanilla's removeCount), which is correct for every take
 * path — normal click, shift-click, number-key swap and drop.
 */
public final class ItemGainTracker {
	private ItemGainTracker() {
	}

	/** Result taken from any furnace-type menu (furnace, blast furnace, smoker). */
	public static void onSmeltTake(Player player, ItemStack stack, int amount) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		// TODO(debug): remove once counters are confirmed working in production.
		ToolMastery.LOGGER.info("[gate-debug] smelt take: {} x{} by {}",
			stack.getItem(), amount, serverPlayer.getName().getString());
		if (stack.is(Items.CHARCOAL)) {
			SkillService.addCount(serverPlayer, SkillTrees.AXE, "make_charcoal", amount);
		}
		if (stack.is(Items.IRON_INGOT) || stack.is(Items.COPPER_INGOT)
				|| stack.is(Items.GOLD_INGOT) || stack.is(Items.NETHERITE_SCRAP)) {
			SkillService.addCount(serverPlayer, SkillTrees.PICKAXE, "smelt_ores", amount);
		}
	}

	/** Result taken from a crafting grid (table or inventory). */
	public static void onCraftTake(Player player, ItemStack stack, int amount) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		// TODO(debug): remove once counters are confirmed working in production.
		ToolMastery.LOGGER.info("[gate-debug] craft take: {} x{} by {}",
			stack.getItem(), amount, serverPlayer.getName().getString());
		if (stack.is(Items.IRON_PICKAXE)) {
			SkillService.addCount(serverPlayer, SkillTrees.PICKAXE, "craft_iron_pickaxe", amount);
		} else if (stack.is(Items.IRON_AXE)) {
			SkillService.addCount(serverPlayer, SkillTrees.AXE, "craft_iron_axe", amount);
		}
	}
}
