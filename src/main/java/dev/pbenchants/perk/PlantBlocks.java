package dev.pbenchants.perk;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Fortune on a crop is the hoe's job. A rule of the mod rather than something
 * anyone buys — it is on for every player from the moment the mod loads.
 *
 * <p>Vanilla crop loot tables read Fortune off whatever tool happens to be in the
 * loot context, so a Fortune III pickaxe triples a potato field just as well as a
 * hoe does, and nobody has ever had a reason to enchant a hoe. This makes the
 * hoe the only tool that carries Fortune to a plant, which is what gives the
 * Ground tree's farming half something to be about.
 *
 * <p>Applied by {@code BlockDropsMixin} at the one choke point every block drop
 * passes through, so it covers a plain swing, Harvest Swing, Diggy Diggy Hole,
 * TNT and command drops alike.
 */
public final class PlantBlocks {
	/**
	 * The plants the rule covers, beyond {@code #minecraft:crops} (wheat, carrots,
	 * potatoes, beetroots, torchflower, pitcher and both stems). Some of these have
	 * no Fortune bonus in vanilla at all; they are listed anyway so the rule reads
	 * as one sentence rather than a list of exceptions.
	 *
	 * <p>Deliberately absent:
	 * <ul>
	 *   <li><b>Leaves.</b> Fortune on leaves is how vanilla scales saplings, apples
	 *       and sticks, and the Axe tree's Fair Harvest and Pruner are built on
	 *       breaking leaves with an axe. Making apples hoe-only would be a silent
	 *       nerf to a class that already shipped.
	 *   <li><b>Grass and ferns.</b> Fortune there yields wheat seeds, and they are
	 *       broken with everything — hoe-gating the early game's seed supply helps
	 *       nobody.
	 * </ul>
	 */
	private static final Set<Block> EXTRA = Set.of(
		Blocks.NETHER_WART,
		Blocks.COCOA,
		Blocks.MELON,
		Blocks.PUMPKIN,
		Blocks.SWEET_BERRY_BUSH,
		Blocks.CAVE_VINES,
		Blocks.CAVE_VINES_PLANT,
		Blocks.SUGAR_CANE
	);

	private PlantBlocks() {
	}

	public static boolean isPlant(BlockState state) {
		return state.is(BlockTags.CROPS) || EXTRA.contains(state.getBlock());
	}

	/**
	 * The tool as the loot table should see it: on a plant, Fortune only survives
	 * on a hoe.
	 *
	 * <p>A copy, never the real stack. The real one has already been charged
	 * durability upstream in {@code ServerPlayerGameMode.destroyBlock}, and
	 * nothing below {@code Block.getDrops} writes to the tool — the loot context
	 * only reads enchantment levels and item predicates off it — so the copy is
	 * invisible to everything except the bonus roll it exists to cancel. Silk
	 * Touch is untouched, so a Silk Touch pickaxe still pops a whole melon.
	 */
	public static ItemInstance stripNonHoeFortune(BlockState state, ItemInstance tool) {
		if (!(tool instanceof ItemStack stack) || stack.isEmpty() || !stack.isEnchanted()) {
			return tool;
		}
		if (stack.is(ItemTags.HOES) || !isPlant(state)) {
			return tool;
		}
		ItemStack copy = stack.copy();
		EnchantmentHelper.updateEnchantments(copy, mutable -> mutable.removeIf(holder -> holder.is(Enchantments.FORTUNE)));
		return copy;
	}
}
