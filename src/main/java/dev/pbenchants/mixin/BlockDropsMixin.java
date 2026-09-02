package dev.pbenchants.mixin;

import dev.pbenchants.perk.PlantBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Block.class)
public class BlockDropsMixin {
	/**
	 * Fortune on a plant is the hoe's job — see {@link PlantBlocks}.
	 *
	 * <p>Every block drop in the game funnels through this static overload, and
	 * it sits below where durability and mining stats were already charged, so
	 * swapping the tool here reaches a plain swing, Harvest Swing, Diggy Diggy
	 * Hole, explosions and command drops with one hook and no duplicated rule.
	 *
	 * <p>No {@code index}: the method takes exactly one {@code ItemInstance}, so
	 * matching by type is unambiguous and survives a parameter being added ahead
	 * of it. (It is static, so parameter index would equal local index — 5.)
	 */
	@ModifyVariable(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;"
		+ "Lnet/minecraft/server/level/ServerLevel;"
		+ "Lnet/minecraft/core/BlockPos;"
		+ "Lnet/minecraft/world/level/block/entity/BlockEntity;"
		+ "Lnet/minecraft/world/entity/Entity;"
		+ "Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
		at = @At("HEAD"), argsOnly = true)
	private static ItemInstance pbenchants$fortuneIsForHoes(ItemInstance tool, BlockState state,
			ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity breaker) {
		return PlantBlocks.stripNonHoeFortune(state, tool);
	}
}
