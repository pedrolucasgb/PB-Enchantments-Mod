package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Rich Vein — vein miner driven by the Rich Vein enchantment:
 *   I: up to 8 connected ores · II: up to 16
 * Stone and deepslate variants count as the same vein. Sneaking disables.
 */
public final class VeinMiner {
	/** Ore family ids: variants that share a family mine together. */
	private static final Map<Block, Integer> ORE_FAMILY = Map.ofEntries(
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

	private static boolean breakingNow = false;

	private VeinMiner() {
	}

	public static boolean isVeinBreaking() {
		return breakingNow;
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (breakingNow || AreaBreak.isAreaBreaking() || TimberScheduler.isTimberBreaking()) {
			return;
		}
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (serverPlayer.isShiftKeyDown()) {
			return;
		}
		Integer family = ORE_FAMILY.get(state.getBlock());
		if (family == null) {
			return;
		}
		ItemStack pickaxe = serverPlayer.getMainHandItem();
		if (!pickaxe.is(ItemTags.PICKAXES)) {
			return;
		}
		int veinLevel = ModEnchantments.level(serverPlayer, pickaxe, ModEnchantments.RICH_VEIN);
		if (veinLevel <= 0) {
			return;
		}
		int limit = veinLevel >= 2 ? 16 : 8;

		// Flood-fill the vein (26-neighborhood, same family).
		ArrayDeque<BlockPos> vein = new ArrayDeque<>();
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(pos);
		visited.add(pos);
		while (!frontier.isEmpty() && vein.size() < limit) {
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
						if (family.equals(ORE_FAMILY.get(serverLevel.getBlockState(next).getBlock()))) {
							vein.add(next);
							frontier.add(next);
						}
					}
				}
			}
		}

		breakingNow = true;
		try {
			for (BlockPos target : vein) {
				ItemStack tool = serverPlayer.getMainHandItem();
				if (!tool.is(ItemTags.PICKAXES)
					|| (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 2)) {
					return;
				}
				serverPlayer.gameMode.destroyBlock(target);
			}
		} finally {
			breakingNow = false;
		}
	}
}
