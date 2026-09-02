package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.track.FarmingTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Furrow Hand — a hoe tills the whole 3x3 around the block it was clicked on.
 *
 * <p>The eight extra tiles cost the hoe nothing. Tilling is not the interesting
 * part of farming and never was; a field goes from grass to farmland in a ninth
 * of the clicks, and the durability stays for the harvest, which is where the
 * class actually plays.
 *
 * <p>Only the blocks vanilla itself would have turned into farmland, and only
 * with nothing sitting on top of them, so a hoe swept along the edge of a field
 * never lifts a crop or eats a torch.
 */
public final class FurrowHand {
	public static final String NODE = "furrow_hand";

	private FurrowHand() {
	}

	/** Called after a successful vanilla till, with {@code centre} already farmland. */
	public static void tillAround(ServerPlayer player, ServerLevel level, BlockPos centre) {
		if (!PerkAccess.owns(player, SkillTrees.GROUND, NODE)) {
			return;
		}
		BlockState farmland = Blocks.FARMLAND.defaultBlockState();
		int tilled = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue;
				}
				BlockPos target = centre.offset(dx, 0, dz);
				if (!tillable(level, target)) {
					continue;
				}
				level.setBlockAndUpdate(target, farmland);
				tilled++;
			}
		}
		if (tilled > 0) {
			level.playSound(null, centre, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
			FarmingTracker.onTilled(player, tilled);
		}
	}

	private static boolean tillable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		boolean ground = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH);
		return ground && level.getBlockState(pos.above()).isAir();
	}
}
