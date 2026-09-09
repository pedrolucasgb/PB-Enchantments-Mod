package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * An enchanted shulker box survives being placed (0.10.0 hotfix).
 *
 * <p>Placing the box stores its enchantments on the block entity like any
 * other leftover component — but vanilla's shulker loot table rebuilds the
 * drop with {@code copy_components} and an <em>include list</em> of exactly
 * four: custom name, container, lock, container loot. Enchantments are not on
 * the list, so an Indestructible box came back bare from one place-and-break.
 *
 * <p>Rather than overriding seventeen loot tables (one per colour, plus every
 * data pack that touches them), the enchantments are put back on the drop the
 * moment the break event fires — the one hook that still holds the block
 * entity in one hand while the fresh drop lies on the ground in the other.
 * Generic on purpose: whatever enchantment a box carries rides along, not
 * just Indestructible.
 */
public final class ShulkerEnchantKeeper {
	private ShulkerEnchantKeeper() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity) {
		if (!(level instanceof ServerLevel serverLevel) || blockEntity == null
			|| !(state.getBlock() instanceof ShulkerBoxBlock)) {
			return;
		}
		ItemEnchantments enchantments = blockEntity.components().get(DataComponents.ENCHANTMENTS);
		if (enchantments == null || enchantments.isEmpty()) {
			return;
		}
		// The drop spawned inside destroyBlock, a call up the stack: it is the
		// box-shaped item entity born this tick, still bare of enchantments.
		for (ItemEntity drop : serverLevel.getEntitiesOfClass(
			ItemEntity.class, new AABB(pos).inflate(1.0), entity -> entity.tickCount <= 1)) {
			ItemStack stack = drop.getItem();
			if (stack.is(ItemTags.SHULKER_BOXES)
				&& stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
				stack.set(DataComponents.ENCHANTMENTS, enchantments);
				drop.setItem(stack);
				return;
			}
		}
	}
}
