package dev.toolmastery.mixin;

import dev.toolmastery.perk.AncientFortune;
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

/**
 * Ancient Fortune: swaps the tool the loot table sees for a Fortune-boosted
 * copy. Sits on the one method every block drop goes through, so it covers
 * hand-mined blocks, Dig Range, Rich Vein and Logic fells alike.
 */
@Mixin(Block.class)
public class BlockDropsMixin {
	@ModifyVariable(
		method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;"
			+ "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;"
			+ "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
		at = @At("HEAD"), argsOnly = true)
	private static ItemInstance toolmastery$ancientFortune(ItemInstance value, BlockState state, ServerLevel level,
	                                                       BlockPos pos, BlockEntity blockEntity, Entity entity,
	                                                       ItemInstance tool) {
		return AncientFortune.boosted(level, value);
	}
}
