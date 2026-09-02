package dev.pbenchants.track;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.GateChecklists;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Feeds gate counters from block-break events. The checklist gates it feeds —
 * distinct ores, distinct woods — hand their bits to {@link GateChecklists},
 * which owns both the roster and the mask counter behind the visible count.
 */
public final class BlockBreakTracker {
	/**
	 * Every biome that grows emerald ore. Vanilla's {@code #is_mountain} is not
	 * that list: it leaves out the groves and the windswept hills, which is where
	 * most players meet their first emerald, so the gate sat at 0/1 for them.
	 */
	private static final TagKey<Biome> EMERALD_MOUNTAIN = TagKey.create(Registries.BIOME,
		Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, "emerald_mountain"));

	private BlockBreakTracker() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		// Leaves are counted for whatever broke them - axe, shears, sword or a
		// bare hand. The gate asks how much canopy you have cleared, not which
		// tool you cleared it with, and only a player break reaches this event,
		// so natural decay never counts.
		if (state.is(BlockTags.LEAVES)) {
			countLeaf(serverPlayer);
		}

		ItemStack held = serverPlayer.getMainHandItem();
		if (held.is(ItemTags.PICKAXES)) {
			trackPickaxe(serverPlayer, serverLevel, pos, state);
		} else if (held.is(ItemTags.AXES)) {
			trackAxe(serverPlayer, state);
		}
	}

	/**
	 * One leaf block cleared by this player. Public because Timber fells its
	 * canopy through {@code Level.destroyBlock}, which never raises the break
	 * event this class otherwise listens to.
	 */
	public static void countLeaf(ServerPlayer player) {
		SkillService.progress(player, SkillTrees.AXE).addCount("break_leaves", 1);
	}

	private static void trackPickaxe(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.PICKAXE);
		progress.addCount("break_total", 1);

		if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
			progress.addCount("break_stone", 1);
		}
		if (state.is(Blocks.DEEPSLATE)) {
			progress.addCount("break_deepslate", 1);
		}

		int oreBit = -1;
		if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) {
			progress.addCount("mine_coal", 1);
			oreBit = 0;
		} else if (state.is(BlockTags.COPPER_ORES)) {
			progress.addCount("mine_copper", 1);
			oreBit = 1;
		} else if (state.is(BlockTags.IRON_ORES)) {
			progress.addCount("mine_iron", 1);
			oreBit = 2;
		} else if (state.is(Blocks.NETHER_GOLD_ORE)) {
			// Caught before the tag: #gold_ores counts the Nether's gold as gold,
			// so leaving this to the branch below spent bit 3 on it and left bit 9
			// unreachable — the checklist could never pass ten. It still feeds the
			// gold counter, as it did when the tag swallowed it.
			progress.addCount("mine_gold", 1);
			oreBit = 9;
		} else if (state.is(BlockTags.GOLD_ORES)) {
			progress.addCount("mine_gold", 1);
			oreBit = 3;
		} else if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
			progress.addCount("mine_redstone", 1);
			oreBit = 4;
		} else if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
			progress.addCount("mine_lapis", 1);
			// lapis feeds the enchanter's tier-2 gate too
			SkillService.progress(player, SkillTrees.ENCHANTER).addCount("mine_lapis", 1);
			oreBit = 5;
		} else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
			progress.addCount("mine_diamond", 1);
			oreBit = 6;
		} else if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
			oreBit = 7;
			if (level.getBiome(pos).is(EMERALD_MOUNTAIN)) {
				progress.addCount("mine_mountain_emerald", 1);
			}
		} else if (state.is(Blocks.NETHER_QUARTZ_ORE)) {
			oreBit = 8;
		} else if (state.is(Blocks.ANCIENT_DEBRIS)) {
			progress.addCount("mine_ancient_debris", 1);
			oreBit = 10;
		}
		if (oreBit >= 0) {
			GateChecklists.tick(progress, "ore_checklist", oreBit);
		}
	}

	public static void onStrip(Level level, Player player, BlockPos pos) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		SkillService.progress(serverPlayer, SkillTrees.AXE).addCount("strip_logs", 1);
	}

	private static void trackAxe(ServerPlayer player, BlockState state) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.AXE);

		if (state.is(BlockTags.LOGS)) {
			progress.addCount("chop_logs", 1);
			progress.addCount("chop_logs_total", 1);

			int woodBit = overworldWoodBit(state);
			if (woodBit >= 0) {
				GateChecklists.tick(progress, "overworld_wood_checklist", woodBit);
			}
			int netherBit = netherWoodBit(state);
			if (netherBit >= 0) {
				GateChecklists.tick(progress, "nether_wood_checklist", netherBit);
			}
		}
	}

	/**
	 * One checklist read twice: tier 2 wants any six of these, tier 4 wants all
	 * nine. Which six come first is the player's to choose.
	 */
	private static int overworldWoodBit(BlockState state) {
		if (state.is(Blocks.OAK_LOG)) return 0;
		if (state.is(Blocks.SPRUCE_LOG)) return 1;
		if (state.is(Blocks.BIRCH_LOG)) return 2;
		if (state.is(Blocks.JUNGLE_LOG)) return 3;
		if (state.is(Blocks.ACACIA_LOG)) return 4;
		if (state.is(Blocks.DARK_OAK_LOG)) return 5;
		if (state.is(Blocks.MANGROVE_LOG)) return 6;
		if (state.is(Blocks.CHERRY_LOG)) return 7;
		if (state.is(Blocks.PALE_OAK_LOG)) return 8;
		return -1;
	}

	private static int netherWoodBit(BlockState state) {
		if (state.is(Blocks.CRIMSON_STEM)) return 0;
		if (state.is(Blocks.WARPED_STEM)) return 1;
		return -1;
	}
}
