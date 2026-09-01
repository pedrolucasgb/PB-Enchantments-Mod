package dev.pbenchants.track;

import java.util.HashSet;
import java.util.Set;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
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
	 * The one line every XP purchase in the mod reports to: the enchanting
	 * table, the anvil, and every skill-tree unlock, tier and enchant bought
	 * with points.
	 *
	 * <p>Counted in <b>points</b>, not levels. A level is not a fixed price —
	 * three levels off a level-40 player is several times the experience it is
	 * off a level-15 one — so a gate measured in levels asked a different
	 * question of every player. Points are the same currency the skill tree
	 * already charges in ({@link dev.pbenchants.skill.XpMath}).
	 *
	 * <p>The counter lives on the Enchanter tree because that is where the gate
	 * that reads it lives; what the points were spent on does not matter.
	 */
	public static void onXpPointsSpent(Player player, int points) {
		if (points > 0 && player instanceof ServerPlayer serverPlayer) {
			SkillService.addCount(serverPlayer, SkillTrees.ENCHANTER, "spend_points", points);
		}
	}

	/**
	 * @param slotId 0-based table slot used (2 = the bottom, level-30 slot)
	 * @param points XP points actually deducted, measured across the enchant
	 *               (0 in creative-like flows is still a valid enchant)
	 */
	public static void onTableEnchant(Player player, ItemStack stack, int slotId, int points) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		TreeProgress progress = SkillService.progress(serverPlayer, SkillTrees.ENCHANTER);
		progress.addCount("enchant_items", 1);
		onXpPointsSpent(serverPlayer, points);
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
