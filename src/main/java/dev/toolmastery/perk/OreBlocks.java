package dev.toolmastery.perk;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * The ore blocks the pickaxe tree cares about, grouped into families: the stone
 * and deepslate variants of an ore share a family id, so a mixed vein mines as
 * one vein and the speed passives treat both alike.
 *
 * <p>26.x dropped the per-ore block tags (only GOLD/IRON/COPPER_ORES survive),
 * so the list is explicit.
 */
public final class OreBlocks {
	private static final Map<Block, Integer> FAMILY = Map.ofEntries(
		Map.entry(Blocks.COAL_ORE, 0), Map.entry(Blocks.DEEPSLATE_COAL_ORE, 0),
		Map.entry(Blocks.COPPER_ORE, 1), Map.entry(Blocks.DEEPSLATE_COPPER_ORE, 1),
		Map.entry(Blocks.IRON_ORE, 2), Map.entry(Blocks.DEEPSLATE_IRON_ORE, 2),
		Map.entry(Blocks.GOLD_ORE, 3), Map.entry(Blocks.DEEPSLATE_GOLD_ORE, 3),
		Map.entry(Blocks.REDSTONE_ORE, 4), Map.entry(Blocks.DEEPSLATE_REDSTONE_ORE, 4),
		Map.entry(Blocks.LAPIS_ORE, 5), Map.entry(Blocks.DEEPSLATE_LAPIS_ORE, 5),
		Map.entry(Blocks.DIAMOND_ORE, 6), Map.entry(Blocks.DEEPSLATE_DIAMOND_ORE, 6),
		Map.entry(Blocks.EMERALD_ORE, 7), Map.entry(Blocks.DEEPSLATE_EMERALD_ORE, 7),
		Map.entry(Blocks.NETHER_QUARTZ_ORE, 8),
		Map.entry(Blocks.NETHER_GOLD_ORE, 9),
		Map.entry(Blocks.ANCIENT_DEBRIS, 10)
	);

	private OreBlocks() {
	}

	/** Family id shared by the variants of one ore, or null when the block is not an ore. */
	@Nullable
	public static Integer family(Block block) {
		return FAMILY.get(block);
	}

	public static boolean isOre(BlockState state) {
		return FAMILY.containsKey(state.getBlock());
	}
}
