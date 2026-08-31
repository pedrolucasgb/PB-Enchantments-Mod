package dev.pbenchants.storage;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Steady Grid — a crafting table keeps what is in its grid when you walk away.
 *
 * <p>Vanilla empties the 3×3 back into your inventory the moment the screen
 * closes, which is fine for one recipe and maddening for twenty: every trip to
 * the chest for the missing ingredient shuffles the layout you just built. With
 * this node the grid is stashed instead, and put straight back the next time
 * you open a table.
 *
 * <p>The stash is deliberately per session and per player, held in memory
 * rather than in the save: a grid that survives a server restart is a
 * duplication bug waiting for an edge case. Logging out empties the stash back
 * into the inventory, so nothing is ever lost — only forgotten.
 */
public final class SteadyGrid {
	private static final Map<UUID, List<ItemStack>> stashes = new HashMap<>();

	private SteadyGrid() {
	}

	public static boolean owns(Player player) {
		return player instanceof ServerPlayer serverPlayer
			&& SkillService.owns(serverPlayer, SkillTrees.ARTISAN, "steady_grid");
	}

	/** Takes the grid aside instead of letting vanilla empty it. */
	public static void stash(Player player, Container grid) {
		List<ItemStack> kept = new ArrayList<>(grid.getContainerSize());
		for (int slot = 0; slot < grid.getContainerSize(); slot++) {
			kept.add(grid.getItem(slot).copy());
			grid.setItem(slot, ItemStack.EMPTY);
		}
		stashes.put(player.getUUID(), kept);
	}

	/** Puts a stashed grid back into a table the player has just opened. */
	public static void restore(Player player, Container grid) {
		List<ItemStack> kept = stashes.remove(player.getUUID());
		if (kept == null) {
			return;
		}
		for (int slot = 0; slot < Math.min(kept.size(), grid.getContainerSize()); slot++) {
			if (grid.getItem(slot).isEmpty()) {
				grid.setItem(slot, kept.get(slot));
			} else if (!kept.get(slot).isEmpty()) {
				giveBack(player, kept.get(slot));
			}
		}
	}

	/** Hands the stash back on logout, so a session boundary never eats items. */
	public static void release(ServerPlayer player) {
		List<ItemStack> kept = stashes.remove(player.getUUID());
		if (kept == null) {
			return;
		}
		for (ItemStack stack : kept) {
			if (!stack.isEmpty()) {
				giveBack(player, stack);
			}
		}
	}

	private static void giveBack(Player player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}
}
