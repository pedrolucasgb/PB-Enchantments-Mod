package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Rich Vein — vein miner driven by the Rich Vein enchantment:
 *   I: up to 8 connected ores · II: up to 16
 * Stone and deepslate variants count as the same vein. Sneaking disables.
 *
 * <p>Rich Vein <b>owns the swing</b>: when it fires, Dig Range stays out of it
 * (see the break chain in {@code PBEnchants}). The two used to stack, and a
 * pickaxe carrying both took the vein <em>and</em> a 3x3 of the stone around the
 * block that started it — which is not what either enchantment says it does, and
 * made "how much does one swing take" impossible to predict. Rich Vein follows
 * the ore and nothing else; it scales with Fortune and Smelt, not with range.
 */
public final class VeinMiner {
	private VeinMiner() {
	}

	/** @deprecated superseded by {@link BreakGuard#busy()}; kept for older callers. */
	@Deprecated
	public static boolean isVeinBreaking() {
		return BreakGuard.busy();
	}

	/**
	 * @return true when Rich Vein claimed this break — the caller must then skip
	 *         Dig Range. True even for a lone ore with no neighbours: the rule is
	 *         "an ore broken by a Rich Vein pickaxe is a vein swing", not "a vein
	 *         swing is one that happened to find company".
	 */
	public static boolean onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (BreakGuard.busy()) {
			return false;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return false;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return false;
		}
		Integer family = OreBlocks.family(state.getBlock());
		if (family == null) {
			return false;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return false;
		}
		int veinLevel = ItemAuthority.effectiveLevel(serverPlayer, pickaxe, ModEnchantments.RICH_VEIN);
		if (veinLevel <= 0) {
			return false;
		}
		int limit = veinLevel >= 2 ? 16 : 8;

		// Flood-fill the vein (26-neighborhood, same family). The limit is
		// tested on every addition, not once per frontier block: a single pop
		// can offer 26 neighbours, and checking only at the top of the loop let
		// a Rich Vein I swing take 33 blocks instead of 8.
		ArrayDeque<BlockPos> vein = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(pos);
		visited.add(pos);
		fill:
		while (!frontier.isEmpty()) {
			BlockPos current = frontier.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos next = current.offset(dx, dy, dz);
						if (!visited.add(next)) {
							continue;
						}
						if (family.equals(OreBlocks.family(serverLevel.getBlockState(next).getBlock()))) {
							vein.add(next);
							frontier.add(next);
							if (vein.size() >= limit) {
								break fill;
							}
						}
					}
				}
			}
		}

		BreakGuard.enter();
		try {
			for (BlockPos target : vein) {
				ItemStack tool = serverPlayer.getMainHandItem();
				if (!tool.is(ItemTags.PICKAXES)
					|| (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 2)) {
					break;
				}
				serverPlayer.gameMode.destroyBlock(target);
			}
		} finally {
			BreakGuard.exit();
		}
		return true;
	}
}
