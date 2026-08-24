package dev.toolmastery.perk;

import com.mojang.datafixers.util.Pair;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The two things an Explorer does with a compass in hand.
 *
 * <p><b>Waypoint Stone</b> (tier 4) — sneak and right-click with a compass to
 * bind the spot you are standing on. The compass gets a real lodestone tracker
 * component, so the needle is drawn by vanilla and works in every dimension the
 * waypoint was set in; sneak-clicking again moves it, and a compass with no
 * waypoint is an ordinary compass again after a grindstone-free right-click on
 * nothing... which is to say, binding is the only thing this ever does.
 *
 * <p><b>World's Memory</b> (capstone) — right-click, standing up, and the
 * compass tells you the bearing and distance to the nearest structure of a kind
 * you have already found. Once per in-game day: it is a memory, not a radar.
 *
 * <p>Both hang off the same item on purpose. The compass is the class's tool,
 * and neither perk needs a keybind that would have to be explained.
 */
public final class Waypoints {
	/** Chunk radius the structure search sweeps. 100 chunks is vanilla's own locate range. */
	private static final int SEARCH_CHUNKS = 100;

	private static final String LAST_MEMORY_DAY = "worlds_memory_day";

	private Waypoints() {
	}

	/**
	 * Handles a compass right-click. Returns true when the perk consumed the
	 * interaction, so the caller can stop vanilla from also handling it.
	 */
	public static boolean onCompassUse(ServerPlayer player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.COMPASS)) {
			return false;
		}
		if (player.isShiftKeyDown()) {
			return bindWaypoint(player, stack);
		}
		return recallStructure(player);
	}

	/** Waypoint Stone: pins the compass to where the player is standing. */
	private static boolean bindWaypoint(ServerPlayer player, ItemStack stack) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, ExplorerPerks.WAYPOINT)) {
			return false;
		}
		BlockPos pos = player.blockPosition();
		// tracked = false: the needle keeps pointing at the spot even though
		// there is no lodestone block there to keep it honest.
		stack.set(DataComponents.LODESTONE_TRACKER,
			new LodestoneTracker(Optional.of(GlobalPos.of(player.level().dimension(), pos)), false));
		player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.2F);
		player.sendSystemMessage(Component.translatable("perk.toolmastery.waypoint.bound",
			Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ())
				.withStyle(ChatFormatting.GOLD)), true);
		return true;
	}

	/** World's Memory: nearest structure of a kind already on the checklist, once a day. */
	private static boolean recallStructure(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, ExplorerPerks.WORLDS_MEMORY)) {
			return false;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.EXPLORER);
		ServerLevel level = player.level();
		int today = (int) (level.getOverworldClockTime() / 24000L);
		if (progress.count(LAST_MEMORY_DAY) == today + 1) {
			player.sendSystemMessage(
				Component.translatable("perk.toolmastery.worlds_memory.spent").withStyle(ChatFormatting.GRAY), true);
			return true;
		}

		List<Holder<Structure>> candidates = seenStructures(level, progress);
		if (candidates.isEmpty()) {
			player.sendSystemMessage(
				Component.translatable("perk.toolmastery.worlds_memory.nothing").withStyle(ChatFormatting.GRAY), true);
			return true;
		}

		Pair<BlockPos, Holder<Structure>> nearest = level.getChunkSource().getGenerator()
			.findNearestMapStructure(level, HolderSet.direct(candidates), player.blockPosition(),
				SEARCH_CHUNKS, false);
		if (nearest == null) {
			player.sendSystemMessage(
				Component.translatable("perk.toolmastery.worlds_memory.nothing").withStyle(ChatFormatting.GRAY), true);
			return true;
		}

		// Only a successful recall costs the day's charge; +1 because 0 is
		// "never used" in a counter map that cannot tell absent from zero.
		progress.counters.put(LAST_MEMORY_DAY, today + 1);

		BlockPos target = nearest.getFirst();
		int distance = (int) Math.round(Math.sqrt(player.blockPosition().distSqr(target)));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
			SoundSource.PLAYERS, 0.7F, 1.0F);
		player.sendSystemMessage(Component.translatable("perk.toolmastery.worlds_memory.found",
			Component.literal(name(nearest.getSecond())).withStyle(ChatFormatting.GOLD),
			Component.literal(compass(player.blockPosition(), target)).withStyle(ChatFormatting.AQUA),
			Component.literal(String.valueOf(distance)).withStyle(ChatFormatting.AQUA)));
		return true;
	}

	/** The structures on this player's checklist, as registry holders of the current world. */
	private static List<Holder<Structure>> seenStructures(ServerLevel level, TreeProgress progress) {
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		List<Holder<Structure>> holders = new ArrayList<>();
		for (String entry : progress.seen) {
			if (!entry.startsWith("struct/")) {
				continue;
			}
			Identifier id = Identifier.tryParse(entry.substring("struct/".length()));
			if (id == null) {
				continue;
			}
			registry.get(ResourceKey.create(Registries.STRUCTURE, id)).ifPresent(holders::add);
		}
		return holders;
	}

	private static String name(Holder<Structure> holder) {
		return holder.unwrapKey()
			.map(key -> key.identifier().getPath().replace('_', ' '))
			.orElse("something");
	}

	/** "north-east" — a bearing a player can act on without a map. */
	private static String compass(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		StringBuilder bearing = new StringBuilder();
		if (Math.abs(dz) > Math.abs(dx) / 2) {
			bearing.append(dz < 0 ? "north" : "south");
		}
		if (Math.abs(dx) > Math.abs(dz) / 2) {
			if (!bearing.isEmpty()) {
				bearing.append('-');
			}
			bearing.append(dx < 0 ? "west" : "east");
		}
		return bearing.isEmpty() ? "right here" : bearing.toString();
	}
}
