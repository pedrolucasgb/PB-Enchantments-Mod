package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillTrees;
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
 *   Logic I                 logs chop at a fixed, tool-blind crawl — see below
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

	/**
	 * What a Logic I axe pays for felling the whole tree in one swing, applied
	 * on top of a destroy speed the axe contributes nothing to (see
	 * {@code PlayerMixin.pbenchants$lockedDigsLikeAHand}).
	 *
	 * <p>It used to be a plain -70% on the axe's own speed, which Efficiency
	 * walked straight through: an Efficiency V diamond axe felled a whole tree
	 * in a third of a second. Now the item's contribution is forced to the
	 * bare-hand 1.0 first — which also means vanilla never adds the
	 * MINING_EFFICIENCY bonus, since that only applies above 1.0 — and this
	 * factor lands on top. Every axe in the game, enchanted or not, therefore
	 * chops a log in about 4.3s, near enough to Mining Fatigue and just about
	 * what felling the same tree log by log would have cost.
	 */
	private static final float LOGIC_1_SLOWDOWN = 0.7F;

	private static final String[] MASONS_GRIP = {"masons_grip_1", "masons_grip_2", "masons_grip_3"};
	private static final String[] LUMBERJACKS_ARMS = {"lumberjacks_arms_1", "lumberjacks_arms_2", "lumberjacks_arms_3"};

	private MiningSpeed() {
	}

	/** Factor to scale this player's destroy speed on this block by (1.0 = unchanged). */
	public static float multiplier(Player player, BlockState state) {
		ItemStack held = player.getMainHandItem();
		// A Logic I chop is a flat rate, not a discount: Lumberjack's Arms must
		// not buy any of it back, or the trade-off would scale away again.
		if (isLogicChop(player, held, state)) {
			return LOGIC_1_SLOWDOWN;
		}
		return pickaxe(player, held, state) * axe(player, held, state);
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
	 * Is this swing the Logic I trade-off — the slow chop that pays for felling
	 * the whole tree at once? Levels 2+ chop at normal speed, and sneaking
	 * (which disables timber) disables the slowdown with it.
	 *
	 * <p>Public because two hooks need the same answer: this class scales the
	 * final speed, and {@code PlayerMixin} throws away the axe's own
	 * contribution first so no amount of Efficiency can outrun it.
	 */
	public static boolean isLogicChop(Player player, ItemStack held, BlockState state) {
		return state.is(BlockTags.LOGS)
			&& !player.isShiftKeyDown()
			&& held.is(ItemTags.AXES)
			&& ItemAuthority.effectiveLevel(player, held, ModEnchantments.LOGIC) == 1;
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
