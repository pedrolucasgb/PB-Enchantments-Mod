package dev.pbenchants.perk;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** What the hoe perks agree a crop is, and what putting one back costs. */
public final class HoeCrops {
	/**
	 * What a harvested crop puts back in the ground. An explicit map rather than
	 * {@code CropBlock.getBaseSeedId}, which is protected — and which would miss
	 * nether wart, the one thing here that is not a {@link CropBlock} at all.
	 * Same shape as {@code TimberScheduler.LOG_TO_SAPLING}.
	 */
	private static final Map<Block, Item> CROP_TO_SEED = Map.ofEntries(
		Map.entry(Blocks.WHEAT, Items.WHEAT_SEEDS),
		Map.entry(Blocks.CARROTS, Items.CARROT),
		Map.entry(Blocks.POTATOES, Items.POTATO),
		Map.entry(Blocks.BEETROOTS, Items.BEETROOT_SEEDS),
		Map.entry(Blocks.NETHER_WART, Items.NETHER_WART),
		Map.entry(Blocks.TORCHFLOWER_CROP, Items.TORCHFLOWER_SEEDS),
		Map.entry(Blocks.PITCHER_CROP, Items.PITCHER_POD)
	);

	private HoeCrops() {
	}

	@Nullable
	public static Item seedFor(Block block) {
		return CROP_TO_SEED.get(block);
	}

	/**
	 * True only at full growth. Everything the hoe half does — area harvest,
	 * replanting, the drop passives — is gated on this, so a wide swing never
	 * costs a player the seedlings they were waiting on.
	 */
	public static boolean isMature(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if (state.is(Blocks.NETHER_WART)) {
			return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
		}
		if (state.is(Blocks.COCOA)) {
			return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
		}
		if (state.is(Blocks.SWEET_BERRY_BUSH)) {
			return state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
		}
		return false;
	}

	/**
	 * What a hoe swing counts as a harvest. Everything {@link #isMature} covers,
	 * plus the three plants that have no growth stage at all — a melon, a pumpkin
	 * and a sugar cane are ripe by existing, so breaking one is always the harvest.
	 * Replanting still only applies to the ones with a seed, which
	 * {@link #seedFor} decides on its own.
	 */
	public static boolean isHarvestable(BlockState state) {
		return isMature(state)
			|| state.is(Blocks.MELON)
			|| state.is(Blocks.PUMPKIN)
			|| state.is(Blocks.SUGAR_CANE);
	}
}
