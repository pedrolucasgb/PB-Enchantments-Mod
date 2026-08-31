package dev.pbenchants.track;

import dev.pbenchants.perk.ExplorerPerks;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Locale;
import java.util.Map;

/**
 * Feeds the Explorer's "places seen" gates: the biome, dimension and structure
 * checklists, sampled once a second from wherever the player is standing.
 *
 * <p>These three do not fit the single-{@code long} bitmask the ore and wood
 * checklists use — the Overworld alone has more than 64 biomes, and the
 * structure list grows with every data pack — so they are kept by name in
 * {@link TreeProgress#seen} and the visible counter is the size of the list.
 *
 * <p>Cartographer is the visible face of the same sample: with the node
 * unlocked, walking into a biome you have never visited prints its name and
 * your coordinates in the action bar, which is also what makes the biome gate
 * legible instead of a number that moves for no apparent reason.
 */
public final class BiomeTracker {
	private BiomeTracker() {
	}

	/** Called once a second per online player. */
	public static void tick(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition();
		TreeProgress progress = SkillService.progress(player, SkillTrees.EXPLORER);

		progress.see("dim", level.dimension().identifier().toString(), "dimension_checklist");

		level.getBiome(pos).unwrapKey().ifPresent(key -> {
			if (progress.see("biome", key.identifier().toString(), "biome_checklist")
				&& ExplorerPerks.owns(player, ExplorerPerks.CARTOGRAPHER)) {
				announce(player, prettyName(key.identifier()), pos);
			}
		});

		Map<Structure, ?> structures = level.structureManager().getAllStructuresAt(pos);
		if (structures.isEmpty()) {
			return;
		}
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		for (Structure structure : structures.keySet()) {
			Identifier id = registry.getKey(structure);
			if (id != null) {
				progress.see("struct", id.toString(), "structure_checklist");
			}
		}
	}

	/** "First time here" — the name of the biome and where you are standing. */
	private static void announce(ServerPlayer player, String name, BlockPos pos) {
		player.sendSystemMessage(Component.translatable("perk.pbenchants.cartographer.new_biome",
			Component.literal(name).withStyle(ChatFormatting.GOLD),
			Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
				.withStyle(ChatFormatting.GRAY)), true);
	}

	/** "minecraft:old_growth_birch_forest" → "Old Growth Birch Forest". */
	private static String prettyName(Identifier id) {
		StringBuilder name = new StringBuilder(id.getPath().length());
		for (String word : id.getPath().split("_")) {
			if (word.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
		}
		return name.toString();
	}
}
