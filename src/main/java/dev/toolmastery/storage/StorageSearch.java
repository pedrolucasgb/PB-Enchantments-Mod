package dev.toolmastery.storage;

import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Seeker's Eye and the Ledger: "where is my iron?", answered without opening a
 * single chest.
 *
 * <p>The three ranks are the same sweep at three settings, so there is one code
 * path rather than three:
 *
 * <ul>
 *   <li><b>I</b> — your own inventory and the container you have open.</li>
 *   <li><b>II</b> — plus every container within 8 blocks, reported as a total
 *       and a count of containers.</li>
 *   <li><b>III</b> — 16 blocks, one line per container with its bearing and
 *       distance, and it reads shulker boxes stored inside those containers.</li>
 * </ul>
 *
 * <p>It is strictly read-only. The Ledger is this query with an empty search
 * string: an index, not a remote inventory.
 */
public final class StorageSearch {
	/** How many result lines are ever produced, however big the room is. */
	public static final int MAX_LINES = 60;

	/** Which rank of Seeker's Eye a player has, and what it may do. */
	public record Reach(int rank, int radius, boolean perContainer, boolean intoShulkers) {
		public boolean any() {
			return rank > 0;
		}
	}

	/** One item found, and where. */
	private record Tally(String name, int inInventory, int inOpen, int nearby, List<String> places) {
	}

	private StorageSearch() {
	}

	/** What the player's Artisan tree entitles them to. Rank 0 means the node is not unlocked. */
	public static Reach reachOf(ServerPlayer player) {
		if (SkillService.owns(player, SkillTrees.ARTISAN, "chest_search_3")) {
			return new Reach(3, 16, true, true);
		}
		if (SkillService.owns(player, SkillTrees.ARTISAN, "chest_search_2")) {
			return new Reach(2, 8, false, false);
		}
		if (SkillService.owns(player, SkillTrees.ARTISAN, "chest_search_1")) {
			return new Reach(1, 0, false, false);
		}
		return new Reach(0, 0, false, false);
	}

	/**
	 * Runs the query and formats it for the panel.
	 *
	 * @param query    what the player typed; empty lists everything (the Ledger)
	 * @param open     the container the player currently has on screen, or null
	 */
	public static List<String> search(ServerPlayer player, String query, @Nullable Container open) {
		Reach reach = reachOf(player);
		if (!reach.any()) {
			return List.of();
		}
		String needle = query.trim().toLowerCase(Locale.ROOT);
		Map<Item, Tally> tallies = new LinkedHashMap<>();

		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			add(tallies, needle, player.getInventory().getItem(slot), Where.INVENTORY, null, reach);
		}
		if (open != null) {
			for (int slot = 0; slot < open.getContainerSize(); slot++) {
				add(tallies, needle, open.getItem(slot), Where.OPEN, null, reach);
			}
		}
		if (reach.radius() > 0) {
			for (ContainerScan.Found found : ContainerScan.nearby(player, reach.radius())) {
				String label = found.name().getString() + " " + bearing(player.blockPosition(), found.pos());
				for (int slot = 0; slot < found.container().getContainerSize(); slot++) {
					add(tallies, needle, found.container().getItem(slot), Where.NEARBY, label, reach);
				}
			}
		}

		List<Tally> ordered = new ArrayList<>(tallies.values());
		ordered.sort(Comparator.comparingInt((Tally tally) ->
			-(tally.inInventory() + tally.inOpen() + tally.nearby())).thenComparing(Tally::name));

		List<String> lines = new ArrayList<>();
		for (Tally tally : ordered) {
			if (lines.size() >= MAX_LINES) {
				lines.add("...and more — narrow the search.");
				break;
			}
			lines.add(headline(tally));
			if (reach.perContainer()) {
				for (String place : tally.places()) {
					if (lines.size() >= MAX_LINES) {
						break;
					}
					lines.add("  " + place);
				}
			}
		}
		return lines;
	}

	private enum Where {
		INVENTORY, OPEN, NEARBY
	}

	/** Folds one stack into the tally, following the nested-shulker rule of rank III. */
	private static void add(Map<Item, Tally> tallies, String needle, ItemStack stack, Where where,
			@Nullable String place, Reach reach) {
		if (stack.isEmpty()) {
			return;
		}
		if (reach.intoShulkers() && where == Where.NEARBY) {
			ItemContainerContents nested = stack.get(DataComponents.CONTAINER);
			if (nested != null) {
				String inside = place == null ? stack.getHoverName().getString()
					: "inside " + stack.getHoverName().getString() + " — " + place;
				nested.nonEmptyItemCopyStream().forEach(inner -> add(tallies, needle, inner, where, inside, reach));
			}
		}
		String name = stack.getHoverName().getString();
		if (!needle.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(needle)) {
			return;
		}
		Tally tally = tallies.computeIfAbsent(stack.getItem(),
			item -> new Tally(name, 0, 0, 0, new ArrayList<>()));
		int count = stack.getCount();
		Tally updated = switch (where) {
			case INVENTORY -> new Tally(tally.name(), tally.inInventory() + count, tally.inOpen(),
				tally.nearby(), tally.places());
			case OPEN -> new Tally(tally.name(), tally.inInventory(), tally.inOpen() + count,
				tally.nearby(), tally.places());
			case NEARBY -> new Tally(tally.name(), tally.inInventory(), tally.inOpen(),
				tally.nearby() + count, tally.places());
		};
		if (where == Where.NEARBY && place != null) {
			mergePlace(updated.places(), place, count);
		}
		tallies.put(stack.getItem(), updated);
	}

	/** Keeps one line per container, summing repeats rather than listing a chest twice. */
	private static void mergePlace(List<String> places, String place, int count) {
		String prefix = place + " x";
		for (int index = 0; index < places.size(); index++) {
			if (places.get(index).startsWith(prefix)) {
				int existing = Integer.parseInt(places.get(index).substring(prefix.length()));
				places.set(index, prefix + (existing + count));
				return;
			}
		}
		places.add(prefix + count);
	}

	private static String headline(Tally tally) {
		int total = tally.inInventory() + tally.inOpen() + tally.nearby();
		StringBuilder line = new StringBuilder(tally.name()).append(" x").append(total);
		List<String> parts = new ArrayList<>();
		if (tally.inInventory() > 0) {
			parts.add(tally.inInventory() + " on you");
		}
		if (tally.inOpen() > 0) {
			parts.add(tally.inOpen() + " here");
		}
		if (tally.nearby() > 0) {
			parts.add(tally.nearby() + " nearby");
		}
		if (parts.size() > 1) {
			line.append(" (").append(String.join(", ", parts)).append(')');
		}
		return line.toString();
	}

	/** "12 blocks NE" — enough to walk to it without a map marker. */
	private static String bearing(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX();
		int dz = to.getZ() - from.getZ();
		int distance = (int) Math.round(Math.sqrt((double) dx * dx + (double) dz * dz));
		StringBuilder compass = new StringBuilder();
		if (dz < -1) {
			compass.append('N');
		} else if (dz > 1) {
			compass.append('S');
		}
		if (dx > 1) {
			compass.append('E');
		} else if (dx < -1) {
			compass.append('W');
		}
		return compass.isEmpty() ? "(right here)" : "(" + distance + " blocks " + compass + ")";
	}
}
