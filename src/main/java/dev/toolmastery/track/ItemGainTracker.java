package dev.toolmastery.track;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
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
		if (stack.is(Items.IRON_PICKAXE)) {
			SkillService.addCount(serverPlayer, SkillTrees.PICKAXE, "craft_iron_pickaxe", amount);
		} else if (stack.is(Items.IRON_AXE)) {
			SkillService.addCount(serverPlayer, SkillTrees.AXE, "craft_iron_axe", amount);
		} else if (stack.is(Items.BOOKSHELF)) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "craft_bookshelves", amount);
		} else if (stack.is(Items.BOOK)) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "craft_books", amount);
		} else if (stack.is(Items.ENCHANTING_TABLE)) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "craft_enchanting_table", amount);
		}
		trackArtisanCraft(serverPlayer, stack, amount);
	}

	/**
	 * The Artisan's crafting gates. Unlike the other classes, which want one
	 * specific item, this tree measures the <em>volume and breadth</em> of what
	 * a player makes: everything feeds {@code craft_total}, containers and tools
	 * have lines of their own, and the recipe checklist counts distinct results
	 * so that a base built out of one recipe does not open tier 4.
	 */
	private static void trackArtisanCraft(ServerPlayer player, ItemStack stack, int amount) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		progress.addCount("craft_total", amount);
		progress.see("recipe", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), "recipe_checklist");

		if (stack.is(Items.CRAFTING_TABLE)) {
			progress.addCount("craft_crafting_table", amount);
		}
		if (stack.is(Items.CHEST) || stack.is(Items.TRAPPED_CHEST) || stack.is(Items.BARREL)) {
			progress.addCount("craft_chests", amount);
		}
		if (stack.is(ItemTags.SHULKER_BOXES)) {
			progress.addCount("craft_shulker_boxes", amount);
		}
		if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS)
			|| stack.is(ItemTags.HOES) || stack.is(ItemTags.SWORDS)) {
			progress.addCount("craft_tools", amount);
		}
		if (stack.is(ItemTags.BOATS)) {
			SkillService.addCount(player, SkillTrees.EXPLORER, "craft_boat", amount);
		}
	}
}
