package dev.toolmastery.network;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.storage.SortMode;
import dev.toolmastery.storage.StorageOps;
import dev.toolmastery.storage.StorageSearch;
import dev.toolmastery.track.StorageTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;

import java.util.List;

/**
 * The server half of every Artisan button.
 *
 * <p>Each action re-checks the node that grants it before doing anything: the
 * client greys its buttons out, but a client is only ever a suggestion. Nothing
 * here trusts a slot index, a radius or a search string it was handed.
 */
public final class ArtisanHandler {
	/** How far Quick Stack and Restock reach. Terraria's rule, in blocks. */
	private static final int REACH = 8;

	private ArtisanHandler() {
	}

	public static void handle(ServerPlayer player, ArtisanActionPayload payload) {
		switch (payload.action()) {
			case SORT_INVENTORY -> sortInventory(player);
			case SORT_CONTAINER -> sortContainer(player);
			case CYCLE_SORT_MODE -> cycleSortMode(player);
			case QUICK_STACK -> quickStack(player);
			case RESTOCK -> restock(player);
			case SEARCH -> search(player, payload.query());
			case TOGGLE_SLOT_LOCK -> toggleLock(player, payload.slot());
		}
	}

	private static void sortInventory(ServerPlayer player) {
		if (!owns(player, "sorters_hand_1")) {
			return;
		}
		if (StorageOps.sortInventory(player, StorageTracker.sortMode(player))) {
			count(player, "sort_actions", 1);
			count(player, "containers_sorted", 1);
			click(player);
		}
	}

	private static void sortContainer(ServerPlayer player) {
		if (!owns(player, "sorters_hand_2")) {
			return;
		}
		Container storage = StorageTracker.storageOf(player.containerMenu);
		if (storage != null && StorageOps.sortContainer(storage, StorageTracker.sortMode(player))) {
			count(player, "sort_actions", 1);
			count(player, "containers_sorted", 1);
			click(player);
		}
	}

	private static void cycleSortMode(ServerPlayer player) {
		if (!owns(player, "sort_profiles")) {
			return;
		}
		SortMode mode = StorageTracker.cycleSortMode(player);
		player.sendSystemMessage(Component.translatable("screen.toolmastery.sort.now", mode.label())
			.withStyle(ChatFormatting.GRAY), true);
	}

	private static void quickStack(ServerPlayer player) {
		if (!owns(player, "hand_of_order")) {
			return;
		}
		StorageOps.Outcome outcome = StorageOps.quickStack(player, REACH);
		report(player, outcome, "perk.toolmastery.hand_of_order.done", "perk.toolmastery.hand_of_order.nothing");
	}

	private static void restock(ServerPlayer player) {
		if (!owns(player, "restock_nearby")) {
			return;
		}
		StorageOps.Outcome outcome = StorageOps.restock(player, REACH);
		report(player, outcome, "perk.toolmastery.restock.done", "perk.toolmastery.restock.nothing");
	}

	private static void search(ServerPlayer player, String query) {
		Container open = StorageTracker.storageOf(player.containerMenu);
		List<String> lines = StorageSearch.search(player, query, open);
		ServerPlayNetworking.send(player, new StorageResultPayload(lines));
	}

	/**
	 * Pinning is the one action with no cost and no reach, so it only needs the
	 * node and a slot index inside the inventory. An out-of-range index is
	 * dropped rather than clamped: a client that sends one is confused, and
	 * guessing what it meant would pin the wrong slot.
	 */
	private static void toggleLock(ServerPlayer player, int slot) {
		if (!owns(player, "slot_lock") || slot < 0 || slot >= player.getInventory().getContainerSize()) {
			return;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARTISAN);
		progress.setSlotLocked(slot, !progress.slotLocked(slot));
	}

	private static void report(ServerPlayer player, StorageOps.Outcome outcome, String okKey, String emptyKey) {
		if (!outcome.didSomething()) {
			player.sendSystemMessage(Component.translatable(emptyKey).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		player.sendSystemMessage(Component.translatable(okKey, outcome.items(), outcome.containers())
			.withStyle(ChatFormatting.GREEN), true);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_INSERT,
			SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	private static void click(ServerPlayer player) {
		player.level().playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS,
			SoundSource.PLAYERS, 0.6F, 1.4F);
	}

	private static boolean owns(ServerPlayer player, String nodeId) {
		return SkillService.owns(player, SkillTrees.ARTISAN, nodeId);
	}

	private static void count(ServerPlayer player, String counterId, int amount) {
		SkillService.addCount(player, SkillTrees.ARTISAN, counterId, amount);
	}
}
