package dev.pbenchants.perk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
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

import java.util.ArrayList;
import java.util.List;

/**
 * An enchanted shulker box survives being placed (0.10.0 hotfix, take two).
 *
 * <p>Placing the box stores its enchantments on the block entity like any
 * other leftover component — but vanilla's shulker loot table rebuilds the
 * drop with {@code copy_components} and an <em>include list</em> of exactly
 * four: custom name, container, lock, container loot. Enchantments are not on
 * the list, so an Indestructible box came back bare from one place-and-break.
 *
 * <p>Rather than overriding seventeen loot tables (one per colour, plus every
 * data pack that touches them), the enchantments are put back on the drop.
 * The catch — and the bug in the first take — is <em>when</em>: Fabric fires
 * the break event at {@code Block.destroy}, which is BEFORE {@code
 * dropResources} has spawned anything, so a same-stack search finds only
 * empty air. Same lesson every drop perk in this mod already learned: the
 * break event queues, the end of the tick collects. Generic on purpose:
 * whatever enchantment a box carries rides along, not just Indestructible.
 */
public final class ShulkerEnchantKeeper {
	private record Pending(ServerLevel level, BlockPos pos, ItemEnchantments enchantments) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private ShulkerEnchantKeeper() {
	}

	/** Break time: the block entity still exists here, the drop does not yet. Remember, don't search. */
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
		PENDING.add(new Pending(serverLevel, pos.immutable(), enchantments));
	}

	/** End of the same tick: every drop has spawned by now, including the box. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Pending pending : PENDING) {
			for (ItemEntity drop : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(1.0), entity -> entity.tickCount <= 1)) {
				ItemStack stack = drop.getItem();
				if (stack.is(ItemTags.SHULKER_BOXES)
					&& stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
					stack.set(DataComponents.ENCHANTMENTS, pending.enchantments());
					drop.setItem(stack);
					break;
				}
			}
		}
		PENDING.clear();
	}
}
