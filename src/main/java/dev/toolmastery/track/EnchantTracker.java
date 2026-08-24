package dev.toolmastery.track;

import java.util.HashSet;
import java.util.Set;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Feeds every enchanter gate that is not plain XP: the table itself, the
 * librarian trades, the anvil, the grindstone and the book collection.
 *
 * <p>The tree deliberately asks for five different rooms rather than five
 * sizes of the same grind — a table enchant, a bought book, a combine, a
 * disenchant and a shelf of distinct books all move different lines.
 */
public final class EnchantTracker {
	/** Width of the "which kinds of gear have you enchanted" mask. */
	private static final int TYPE_BITS = 0b1_1111_1111;

	private EnchantTracker() {
	}

	/**
	 * @param slotId 0-based table slot used (2 = the bottom, level-30 slot)
	 * @param levels XP levels actually deducted (slotId + 1; 0 in creative-like
	 *               flows is still a valid enchant)
	 */
	public static void onTableEnchant(Player player, ItemStack stack, int slotId, int levels) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		TreeProgress progress = SkillService.progress(serverPlayer, SkillTrees.ENCHANTER);
		progress.addCount("enchant_items", 1);
		if (levels > 0) {
			progress.addCount("spend_levels", levels);
		}
		if (slotId == 2) {
			progress.addCount("max_slot_enchants", 1);
		}
		int typeBit = typeBit(stack);
		if (typeBit >= 0) {
			int mask = (progress.count("enchant_type_checklist_mask") | (1 << typeBit)) & TYPE_BITS;
			progress.counters.put("enchant_type_checklist_mask", mask);
			progress.counters.put("enchant_type_checklist", Integer.bitCount(mask));
		}
	}

	/** An enchanted book taken out of a villager's trade slot. */
	public static void onMerchantTake(Player player, ItemStack stack, int amount) {
		if (player instanceof ServerPlayer serverPlayer && stack.is(Items.ENCHANTED_BOOK)) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "buy_enchanted_books", amount);
		}
	}

	/** An anvil result taken where the sacrifice actually carried enchantments. */
	public static void onAnvilCombine(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "anvil_combines", 1);
			SkillService.addCount(serverPlayer, SkillTrees.ARTISAN, "anvil_combines", 1);
		}
	}

	/** A grindstone result taken where an input actually carried enchantments. */
	public static void onGrindstoneDisenchant(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "grindstone_disenchants", 1);
		}
	}

	/**
	 * "How wide is your library?": once a second, count the distinct
	 * enchantments sitting on enchanted books in the inventory and keep the
	 * best shelf ever assembled — the same rule as the axe's sapling gate.
	 */
	public static void scanBookChecklist(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		Set<Holder<Enchantment>> kinds = new HashSet<>();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.is(Items.ENCHANTED_BOOK)) {
				kinds.addAll(EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet());
			}
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ENCHANTER);
		if (kinds.size() > progress.count("enchanted_book_checklist")) {
			progress.counters.put("enchanted_book_checklist", kinds.size());
		}
	}

	/** Which slot of the gear checklist an enchanted item ticks, or -1. */
	private static int typeBit(ItemStack stack) {
		if (stack.isEmpty()) {
			return -1;
		}
		if (stack.is(ItemTags.SWORDS)) return 0;
		if (stack.is(ItemTags.PICKAXES)) return 1;
		if (stack.is(ItemTags.AXES)) return 2;
		if (stack.is(ItemTags.SHOVELS)) return 3;
		if (stack.is(ItemTags.HOES)) return 4;
		if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
			|| stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) return 5;
		if (stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)) return 6;
		if (stack.is(Items.BOOK)) return 7;
		return 8;
	}
}
