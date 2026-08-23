package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every skill that changes how fast a block breaks, folded into one factor:
 *   Mason's Grip I-III      +20% / +40% / +60% on stone, deepslate and ores
 *   Obsidian Breaker        +50% on obsidian and crying obsidian
 *   Lumberjack's Arms I-III +25% / +50% / +75% on axe-mineable blocks
 *   Logic I                 -70% on logs, what level 1 pays for the instant fell
 *
 * <p>None of these can be a vanilla {@code block_break_speed} attribute: the
 * bonus depends on the block being mined, so they ride on a
 * {@code Player.getDestroySpeed} mixin instead. That method is exactly what
 * {@code BlockBehaviour.getDestroyProgress} divides the block hardness by, so
 * the factor lands on the real breaking speed — on the client, which draws the
 * cracking animation, and on the server, which validates the break.
 *
 * <p>This is deliberately the only place that knows the numbers: the mixin
 * applies it and {@code /mastery debug speed} reports it, so what the player is
 * told and what the game does cannot drift apart.
 */
public final class MiningSpeed {
	/**
	 * Raised from +10%/+15% per rank on 2026-08-23. At the old values a rank III
	 * shaved ~0.07s off a 0.28s stone break — real in the arithmetic, invisible
	 * in the hand, and gone entirely under an Efficiency tool where the whole
	 * break already fits in a tick or two.
	 */
	private static final float MASONS_GRIP_STEP = 0.20F;
	private static final float OBSIDIAN_BREAKER_BONUS = 0.50F;
	private static final float AXE_SPEED_PER_RANK = 0.25F;

	/** What a Logic I axe pays for felling the whole tree in one swing. */
	private static final float LOGIC_1_SLOWDOWN = 0.3F;

	private static final String[] MASONS_GRIP = {"masons_grip_1", "masons_grip_2", "masons_grip_3"};
	private static final String[] LUMBERJACKS_ARMS = {"lumberjacks_arms_1", "lumberjacks_arms_2", "lumberjacks_arms_3"};

	private MiningSpeed() {
	}

	/** Factor to scale this player's destroy speed on this block by (1.0 = unchanged). */
	public static float multiplier(Player player, BlockState state) {
		ItemStack held = player.getMainHandItem();
		return pickaxe(player, held, state) * axe(player, held, state) * logicSlowdown(player, held, state);
	}

	/** Mason's Grip and Obsidian Breaker. The two target sets never overlap. */
	private static float pickaxe(Player player, ItemStack held, BlockState state) {
		if (!held.is(ItemTags.PICKAXES)) {
			return 1.0F;
		}
		if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
			return PerkAccess.owns(player, SkillTrees.PICKAXE, "obsidian_breaker")
				? 1.0F + OBSIDIAN_BREAKER_BONUS
				: 1.0F;
		}
		if (!isMasonTarget(state)) {
			return 1.0F;
		}
		return 1.0F + MASONS_GRIP_STEP * masonsGripRank(player);
	}

	private static float axe(Player player, ItemStack held, BlockState state) {
		if (!state.is(BlockTags.MINEABLE_WITH_AXE) || !held.is(ItemTags.AXES)) {
			return 1.0F;
		}
		return 1.0F + AXE_SPEED_PER_RANK * lumberjacksArmsRank(player);
	}

	/**
	 * Logic level 1 trades chop speed for the instant fell: breaking a log with
	 * a Logic I axe is noticeably slower. Levels 2+ chop at normal speed.
	 * Sneaking (which disables timber) also disables the slowdown.
	 */
	private static float logicSlowdown(Player player, ItemStack held, BlockState state) {
		if (!state.is(BlockTags.LOGS) || player.isShiftKeyDown() || !held.is(ItemTags.AXES)) {
			return 1.0F;
		}
		return ModEnchantments.level(player, held, ModEnchantments.LOGIC) == 1 ? LOGIC_1_SLOWDOWN : 1.0F;
	}

	/** Stone, granite, diorite, andesite, tuff, deepslate (BASE_STONE_OVERWORLD) plus every ore. */
	private static boolean isMasonTarget(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || OreBlocks.isOre(state);
	}

	public static int masonsGripRank(Player player) {
		return PerkAccess.rank(player, SkillTrees.PICKAXE, MASONS_GRIP);
	}

	public static int lumberjacksArmsRank(Player player) {
		return PerkAccess.rank(player, SkillTrees.AXE, LUMBERJACKS_ARMS);
	}
}
