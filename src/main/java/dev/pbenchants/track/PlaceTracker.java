package dev.pbenchants.track;

import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import dev.pbenchants.storage.ContainerScan;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Feeds the Artisan's "you built the room before you organised it" gates:
 * containers placed, and the checklist of distinct workstations set up.
 *
 * <p>Both read block placement rather than crafting, on purpose — a chest
 * inside a chest is not storage, a chest on the floor is.
 */
public final class PlaceTracker {
	/**
	 * The workstation checklist: eight of these and the tier-2 gate opens. It is
	 * deliberately a spread of rooms rather than eight ways to say "crafting
	 * table" — a player who has eight of them has actually built a base.
	 */
	private static final List<Block> WORKSTATIONS = List.of(
		Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER,
		Blocks.ANVIL, Blocks.ENCHANTING_TABLE, Blocks.SMITHING_TABLE, Blocks.STONECUTTER,
		Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE, Blocks.GRINDSTONE,
		Blocks.BREWING_STAND, Blocks.CAMPFIRE, Blocks.COMPOSTER, Blocks.LECTERN
	);

	private PlaceTracker() {
	}

	/** A block the player just placed. Called from the block-item mixin. */
	public static void onPlace(Player player, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		TreeProgress progress = SkillService.progress(serverPlayer, SkillTrees.ARTISAN);

		if (ContainerScan.isStorageBlock(state)) {
			progress.addCount("place_containers", 1);
		}
		if (WORKSTATIONS.contains(state.getBlock())) {
			progress.see("workstation", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
				"workstation_checklist");
		}
	}
}
