package dev.toolmastery.perk;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Auto Block — the Artisan's answer to a backpack full of loose ore money.
 * Nine of any ore material pack themselves into the block the moment the
 * ninth arrives: ingots, raw ore, coal, diamonds, emeralds, redstone, lapis,
 * netherite. Quartz is the deliberate exception — four to a block, not nine,
 * and its block is a building material people want as quartz. Nuggets pack
 * one step down the same ladder: nine gold or iron nuggets become an ingot,
 * which can then ride the ladder up to a block.
 *
 * <p>Driven from the once-a-second slow tick rather than an inventory hook:
 * "within a second of the ninth" is indistinguishable from instant in play,
 * and one scan a second costs nothing. Pinned slots (the Artisan's own slot
 * lock) are never touched — a locked stack of ingots stays exactly as its
 * owner arranged it — and the packing order runs nuggets before ingots so a
 * nugget windfall cascades all the way up in a single pass.
 */
public final class AutoBlock {
	public static final String AUTO_BLOCK = "auto_block";

	private record Packing(Item from, Item into) {
	}

	/**
	 * Nuggets first, then ingots and gems: the list is also the cascade order,
	 * so freshly packed ingots are already in the bag when their own packing
	 * is counted. Copper's block lives in a weathering collection rather than
	 * on {@link Items}, so it comes out of the registry by id.
	 */
	private static final List<Packing> PACKINGS = List.of(
		new Packing(Items.GOLD_NUGGET, Items.GOLD_INGOT),
		new Packing(Items.IRON_NUGGET, Items.IRON_INGOT),
		new Packing(Items.IRON_INGOT, Items.IRON_BLOCK),
		new Packing(Items.GOLD_INGOT, Items.GOLD_BLOCK),
		new Packing(Items.COPPER_INGOT,
			BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "copper_block"))),
		new Packing(Items.RAW_IRON, Items.RAW_IRON_BLOCK),
		new Packing(Items.RAW_GOLD, Items.RAW_GOLD_BLOCK),
		new Packing(Items.RAW_COPPER, Items.RAW_COPPER_BLOCK),
		new Packing(Items.COAL, Items.COAL_BLOCK),
		new Packing(Items.REDSTONE, Items.REDSTONE_BLOCK),
		new Packing(Items.LAPIS_LAZULI, Items.LAPIS_BLOCK),
		new Packing(Items.DIAMOND, Items.DIAMOND_BLOCK),
		new Packing(Items.EMERALD, Items.EMERALD_BLOCK),
		new Packing(Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK)
	);

	private AutoBlock() {
	}

	/** Called once a second per online player. */
	public static void slowTick(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.ARTISAN, AUTO_BLOCK)) {
			return;
		}
		TreeProgress artisan = SkillService.progress(player, SkillTrees.ARTISAN);
		NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();

		for (Packing packing : PACKINGS) {
			int count = 0;
			for (int slot = 0; slot < items.size(); slot++) {
				if (!artisan.slotLocked(slot) && plain(items.get(slot), packing.from())) {
					count += items.get(slot).getCount();
				}
			}
			while (count >= 9) {
				int toRemove = 9;
				for (int slot = 0; slot < items.size() && toRemove > 0; slot++) {
					if (artisan.slotLocked(slot)) {
						continue;
					}
					ItemStack stack = items.get(slot);
					if (!plain(stack, packing.from())) {
						continue;
					}
					int take = Math.min(toRemove, stack.getCount());
					stack.shrink(take);
					toRemove -= take;
				}
				count -= 9;
				ItemStack block = new ItemStack(packing.into());
				if (!player.addItem(block)) {
					player.drop(block, false);
				}
			}
		}
	}

	/**
	 * Only unmodified stacks pack. A renamed or enchanted ingot is somebody's
	 * keepsake or somebody's trick — either way, melting it into a block would
	 * silently destroy the difference.
	 */
	private static boolean plain(ItemStack stack, Item item) {
		return stack.is(item) && stack.getComponentsPatch().isEmpty();
	}
}
