package dev.toolmastery.perk;

import dev.toolmastery.skill.NodeOwnership;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pickaxe speed passives, applied on top of the vanilla destroy speed:
 *   Mason's Grip I-III  +10% / +20% / +30% on stone, deepslate and ores
 *   Obsidian Breaker    +50% on obsidian and crying obsidian
 *
 * <p>Both need a pickaxe in hand. The vanilla {@code block_break_speed}
 * attribute cannot express this because the bonus is conditional on the block
 * being mined, so it rides on a {@code Player.getDestroySpeed} mixin instead.
 * The two target sets never overlap: at most one bonus applies to a block.
 */
public final class MiningSpeed {
	private static final float MASONS_GRIP_STEP = 0.10F;
	private static final float OBSIDIAN_BREAKER_BONUS = 0.50F;

	private MiningSpeed() {
	}

	/** Factor to scale this player's destroy speed on this block by (1.0 = unchanged). */
	public static float multiplier(Player player, BlockState state) {
		if (!player.getMainHandItem().is(ItemTags.PICKAXES)) {
			return 1.0F;
		}
		if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
			return NodeOwnership.owns(player, SkillTrees.PICKAXE, "obsidian_breaker")
				? 1.0F + OBSIDIAN_BREAKER_BONUS
				: 1.0F;
		}
		if (!isMasonTarget(state)) {
			return 1.0F;
		}
		return 1.0F + MASONS_GRIP_STEP * masonsGripLevel(player);
	}

	/** Stone, granite, diorite, andesite, tuff, deepslate (BASE_STONE_OVERWORLD) plus every ore. */
	private static boolean isMasonTarget(BlockState state) {
		return state.is(BlockTags.BASE_STONE_OVERWORLD) || OreBlocks.isOre(state);
	}

	private static int masonsGripLevel(Player player) {
		if (NodeOwnership.owns(player, SkillTrees.PICKAXE, "masons_grip_3")) {
			return 3;
		}
		if (NodeOwnership.owns(player, SkillTrees.PICKAXE, "masons_grip_2")) {
			return 2;
		}
		if (NodeOwnership.owns(player, SkillTrees.PICKAXE, "masons_grip_1")) {
			return 1;
		}
		return 0;
	}
}
