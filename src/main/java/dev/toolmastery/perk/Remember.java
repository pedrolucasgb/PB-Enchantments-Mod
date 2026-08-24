package dev.toolmastery.perk;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Remember — you respawn holding a note with the coordinates of your last death.
 *
 * <p>A named slip of paper carrying the position, the dimension and the in-game
 * day. It replaces the previous one each time rather than accumulating, so it
 * never becomes clutter, and it is a plain piece of paper with components on it
 * — no new item registration, so it stacks nowhere, burns, and can be thrown
 * away like any other note.
 *
 * <p>The coordinates ride in the tree's counter map between death and respawn,
 * which means they survive the respawn itself and a relog on the death screen.
 */
public final class Remember {
	private static final String DEATH_X = "last_death_x";
	private static final String DEATH_Y = "last_death_y";
	private static final String DEATH_Z = "last_death_z";
	private static final String DEATH_DAY = "last_death_day";
	private static final String DEATH_PENDING = "last_death_pending";
	private static final String DEATH_DIMENSION = "deathdim";

	private Remember() {
	}

	/** Stashes where the player fell. Called from the death event, before the respawn. */
	public static void onDeath(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, ExplorerPerks.REMEMBER)) {
			return;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.EXPLORER);
		progress.counters.put(DEATH_X, player.blockPosition().getX());
		progress.counters.put(DEATH_Y, player.blockPosition().getY());
		progress.counters.put(DEATH_Z, player.blockPosition().getZ());
		progress.counters.put(DEATH_DAY, (int) (player.level().getOverworldClockTime() / 24000L));
		progress.counters.put(DEATH_PENDING, 1);
		// The dimension is a name, not a number, so it goes in the name-based
		// ledger; the "deathdim/" prefix is never counted by a gate.
		progress.seen.removeIf(entry -> entry.startsWith(DEATH_DIMENSION + "/"));
		progress.seen.add(DEATH_DIMENSION + "/" + player.level().dimension().identifier().toString());
	}

	/** Hands over the note. Called after the player is back in the world. */
	public static void onRespawn(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.EXPLORER);
		if (progress.count(DEATH_PENDING) != 1) {
			return;
		}
		progress.counters.put(DEATH_PENDING, 0);
		if (!SkillService.owns(player, SkillTrees.EXPLORER, ExplorerPerks.REMEMBER)) {
			return;
		}

		String dimension = "unknown";
		for (String entry : progress.seen) {
			if (entry.startsWith(DEATH_DIMENSION + "/")) {
				dimension = entry.substring(DEATH_DIMENSION.length() + 1);
			}
		}

		ItemStack note = new ItemStack(Items.PAPER);
		note.set(DataComponents.CUSTOM_NAME, Component.translatable("item.toolmastery.death_note")
			.withStyle(ChatFormatting.GOLD));
		note.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal("X " + progress.count(DEATH_X)
					+ "  Y " + progress.count(DEATH_Y)
					+ "  Z " + progress.count(DEATH_Z))
				.withStyle(ChatFormatting.GRAY),
			Component.literal(dimension).withStyle(ChatFormatting.DARK_GRAY),
			Component.literal("Day " + progress.count(DEATH_DAY)).withStyle(ChatFormatting.DARK_GRAY)
		)));

		if (!player.getInventory().add(note)) {
			player.drop(note, false);
		}
	}
}
