package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
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
	 * What goes back in the ground after harvesting {@code harvested}. Every
	 * crop replants as itself except the torchflower: the seedling is
	 * {@code TORCHFLOWER_CROP}, but its last growth stage is a different block
	 * altogether — the {@code TORCHFLOWER} flower — and that is the one the hoe
	 * harvests. Replanting the flower would skip the growing.
	 */
	public static Block replantBlock(Block harvested) {
		return harvested == Blocks.TORCHFLOWER ? Blocks.TORCHFLOWER_CROP : harvested;
	}

	/**
	 * Where a plant is rooted: for the pitcher plant — two blocks tall once it
	 * is grown — the lower half, whichever half was hit; for everything else
	 * the block itself. Vanilla drops the lower half's loot when the upper is
	 * broken, so the root is where both the drops and the replant belong.
	 */
	public static BlockPos rootOf(BlockState state, BlockPos pos) {
		if (state.is(Blocks.PITCHER_CROP) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER) {
			return pos.below();
		}
		return pos;
	}

	/**
	 * True only at full growth. Everything the hoe half does — area harvest,
	 * replanting, the drop passives — is gated on this, so a wide swing never
	 * costs a player the seedlings they were waiting on.
	 *
	 * <p>Two plants do not fit {@code CropBlock.isMaxAge}. The torchflower crop
	 * reports "max age" at its second stage, but that stage still only drops
	 * seeds — the harvest is the {@code TORCHFLOWER} flower it turns into next,
	 * so the seedling is never mature and the flower always is. The pitcher
	 * crop is not a {@code CropBlock} at all; it is a double plant with an age
	 * of its own, ripe at 4.
	 */
	public static boolean isMature(BlockState state) {
		if (state.is(Blocks.TORCHFLOWER_CROP)) {
			return false;
		}
		if (state.is(Blocks.TORCHFLOWER)) {
			return true;
		}
		if (state.is(Blocks.PITCHER_CROP)) {
			return state.getValue(PitcherCropBlock.AGE) >= PitcherCropBlock.MAX_AGE;
		}
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

	/**
	 * A planted crop that has not finished growing — what Green Thumb refuses
	 * to trample. The farmland crops, the torchflower seedling at every stage
	 * (its bloom is a different block, see {@link #isMature}), the pitcher
	 * plant below age 4, and nether wart below its last stage. Cane, cocoa and
	 * berries stay out: cane in particular is harvested by cutting the column
	 * mid-stalk, and a guard there would fight the only way to farm it.
	 */
	public static boolean isGrowing(BlockState state) {
		if (state.is(Blocks.TORCHFLOWER_CROP) || state.is(Blocks.PITCHER_CROP) || state.is(Blocks.NETHER_WART)) {
			return !isMature(state);
		}
		return state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state);
	}
}
