package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Flat Earth — the shovel's area dig, driven by the enchantment of the same name:
 *   I   the block above the one you hit and the block below it
 *   II  3x2: the two beside it and the three below
 *   III the full 3x3 on the plane
 *
 * <p>The plane is the one Dig Range taught: a steep pitch means you are working a
 * floor, so it is horizontal; otherwise it is the wall you are facing. Sneaking
 * disables, extras respect tool tier, and — as in {@link AreaBreak} — an extra
 * block has to be about as hard as the one you actually swung at.
 *
 * <p><b>The floor rule.</b> Nothing here is ever broken below the block the
 * player is standing on, and never that block itself ({@link GroundLevel}).
 * Digging down at your own feet the rest of the layer is fair game, so a swing
 * clears the ring around you and leaves the one tile you are standing on: you
 * step off it and take it with the next swing. Aiming a layer lower yields
 * nothing at all, on purpose — this is the enchantment that cannot drop you into
 * a cave you did not see.
 *
 * <p>Strictly a shovel effect. As with Dig Range and its pickaxe tag, the
 * {@code #minecraft:mineable/shovel} check is what keeps it from turning into a
 * pickaxe — {@link ServerPlayer#hasCorrectToolForDrops} alone lets stone through,
 * since nothing that drops without a required tool ever fails it.
 */
public final class FlatEarth {
	/** Same grace {@link AreaBreak} uses, and for the same reason. */
	private static final float GRACE_TICKS = 15.0F;

	private FlatEarth() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BreakGuard.busy()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return;
		}
		ItemStack shovel = serverPlayer.getMainHandItem();
		if (!shovel.is(ItemTags.SHOVELS)) {
			return;
		}
		if (!digsWithShovel(serverLevel, pos, state, serverPlayer)) {
			return;
		}
		int rangeLevel = ItemAuthority.effectiveLevel(serverPlayer, shovel, ModEnchantments.FLAT_EARTH);
		if (rangeLevel <= 0) {
			return;
		}

		float budgetTicks = ticksToBreak(serverLevel, pos, state, serverPlayer) + GRACE_TICKS;
		BlockPos support = GroundLevel.support(serverPlayer);
		PlaneGrid grid = PlaneGrid.facing(serverPlayer);

		BreakGuard.enter();
		try {
			for (int[] offset : mask(rangeLevel)) {
				if (shovelAboutToBreak(serverPlayer)) {
					return;
				}
				BlockPos target = grid.at(pos, offset[0], offset[1]);
				if (!GroundLevel.allowed(support, target)) {
					continue; // never below the floor, never the block holding you up
				}
				BlockState targetState = serverLevel.getBlockState(target);
				if (!digsWithShovel(serverLevel, target, targetState, serverPlayer)
					|| ticksToBreak(serverLevel, target, targetState, serverPlayer) > budgetTicks) {
					continue;
				}
				serverPlayer.gameMode.destroyBlock(target);
			}
		} finally {
			BreakGuard.exit();
		}
	}

	/**
	 * Offsets in the grid's own {@code (horizontal, vertical)} basis, excluding
	 * the block vanilla already broke. "Vertical" is up in a wall, and away from
	 * the player in a floor — which is why level I still does something useful in
	 * both, rather than aiming its whole pair at the layer the floor rule forbids.
	 */
	private static List<int[]> mask(int rangeLevel) {
		return switch (rangeLevel) {
			case 1 -> List.of(new int[]{0, 1}, new int[]{0, -1});
			case 2 -> List.of(
				new int[]{-1, 0}, new int[]{1, 0},
				new int[]{-1, -1}, new int[]{0, -1}, new int[]{1, -1});
			default -> List.of(
				new int[]{-1, 1}, new int[]{0, 1}, new int[]{1, 1},
				new int[]{-1, 0}, new int[]{1, 0},
				new int[]{-1, -1}, new int[]{0, -1}, new int[]{1, -1});
		};
	}

	/**
	 * A block this shovel should be digging. Farmland is the one deliberate
	 * exclusion: it is shovel-mineable, so a Flat Earth III swing at the edge of
	 * a field would eat the crops of the very tree this enchantment belongs to.
	 */
	private static boolean digsWithShovel(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
		return !state.isAir()
			&& !state.is(Blocks.FARMLAND)
			&& state.is(BlockTags.MINEABLE_WITH_SHOVEL)
			&& state.getDestroySpeed(level, pos) >= 0
			&& player.hasCorrectToolForDrops(state);
	}

	/** See {@code AreaBreak.ticksToBreak}: vanilla's own progress-per-tick. */
	private static float ticksToBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
		float progress = state.getDestroyProgress(player, level, pos);
		if (!(progress > 0.0F)) {
			return Float.MAX_VALUE;
		}
		return 1.0F / progress;
	}

	private static boolean shovelAboutToBreak(ServerPlayer player) {
		ItemStack shovel = player.getMainHandItem();
		return !shovel.is(ItemTags.SHOVELS)
			|| (shovel.isDamageableItem() && shovel.getDamageValue() >= shovel.getMaxDamage() - 2);
	}
}
