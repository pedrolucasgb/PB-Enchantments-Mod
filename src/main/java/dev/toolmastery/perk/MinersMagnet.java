package dev.toolmastery.perk;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * Miner's Magnet — drops from blocks broken with a pickaxe go straight into the
 * player's inventory.
 *
 * <p>Like Melt, breaks are queued and collected at the END of the same server
 * tick, once every drop entity has actually spawned. Ticking after
 * {@link MeltHandler} is what makes the two compose: the magnet picks up the
 * ingots Melt just produced, not the raw ore it replaced. Dig Range and Rich
 * Vein compose for free — each extra block they break fires its own break event
 * and gets queued the same way.
 *
 * <p>Anything that does not fit in the inventory is left on the ground.
 */
public final class MinersMagnet {
	/** Same reach Melt uses — drops land within a block of the broken position. */
	private static final double RADIUS = 1.5;

	private record Pending(ServerLevel level, BlockPos pos, ServerPlayer player) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private MinersMagnet() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!serverPlayer.getMainHandItem().is(ItemTags.PICKAXES)) {
			return;
		}
		if (!SkillService.owns(serverPlayer, SkillTrees.PICKAXE, "miners_magnet")) {
			return;
		}
		PENDING.add(new Pending(serverLevel, pos, serverPlayer));
	}

	/** Called at the end of every server tick, after Melt has converted its drops. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Pending pending : PENDING) {
			ServerPlayer player = pending.player();
			if (player.isRemoved() || player.level() != pending.level()) {
				continue;
			}
			for (ItemEntity drop : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(RADIUS),
				entity -> entity.tickCount <= 1 && !entity.isRemoved())) {
				collect(player, drop);
			}
		}
		PENDING.clear();
	}

	/**
	 * Moves as much of the drop as fits into the inventory. Mirrors vanilla's
	 * pickup bookkeeping (ItemEntity.playerTouch): pickup animation, statistic
	 * and the item-pickup criterion, so advancements keep firing.
	 */
	private static void collect(ServerPlayer player, ItemEntity drop) {
		ItemStack stack = drop.getItem();
		int before = stack.getCount();
		if (before <= 0) {
			return;
		}
		// Inventory.add drains the stack in place and leaves whatever did not fit.
		player.getInventory().add(stack);
		int taken = before - stack.getCount();
		if (taken <= 0) {
			return;
		}
		player.take(drop, taken);
		if (stack.isEmpty()) {
			drop.discard();
			stack.setCount(before); // vanilla restores it so the stat and criterion see the real count
		} else {
			drop.setItem(stack);
		}
		player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), taken);
		player.onItemPickup(drop);
	}
}
