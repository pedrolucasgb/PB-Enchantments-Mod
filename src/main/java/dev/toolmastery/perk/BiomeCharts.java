package dev.toolmastery.perk;

import com.mojang.datafixers.util.Pair;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillNode;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.XpMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Biome Chart — the Explorer buys a real, filled map for XP: the mod's first
 * skill whose purchase drops an item in the inventory rather than switching
 * an ability on.
 *
 * <p>Rank I is a cheap gamble: any biome this dimension can generate, wherever
 * the nearest one happens to be. Rank II is the cartographer's version — only
 * biomes the player has never set foot in qualify (read from the same
 * {@code seen} list the biome checklist counts), so every map is a guaranteed
 * discovery, and its price climbs with the tiers unlocked, because the last
 * few missing biomes are exactly the expensive ones to have found for you.
 *
 * <p>The search runs before the charge: a biome the locator cannot find
 * within range costs nothing, and the failure says so. The map itself is a
 * vanilla filled map at scale 2 with a biome-preview render and an X on the
 * target, named after the biome it points to — in the client's language,
 * because the name rides as a translatable component.
 */
public final class BiomeCharts {
	public static final String RANK_1 = "biome_chart_1";
	public static final String RANK_2 = "biome_chart_2";

	/** One cheap purchase: five levels' worth of points. */
	private static final int BASE_COST = XpMath.pointsForLevel(5);

	/** Same reach and sampling steps as vanilla's /locate biome. */
	private static final int LOCATE_RADIUS = 6400;
	private static final int LOCATE_STEP_HORIZONTAL = 32;
	private static final int LOCATE_STEP_VERTICAL = 64;

	/** How many different biomes are tried before the purchase gives up, unspent. */
	private static final int ATTEMPTS = 3;

	private BiomeCharts() {
	}

	/** True for the two nodes whose owned-state Buy button this class serves. */
	public static boolean isChart(String nodeId) {
		return RANK_1.equals(nodeId) || RANK_2.equals(nodeId);
	}

	/**
	 * The price in XP points. Shared verbatim by the server (to charge) and
	 * the screen (to label the button), so the two can never disagree.
	 */
	public static int cost(String nodeId, int unlockedTiers) {
		return RANK_2.equals(nodeId) ? BASE_COST * Math.max(1, unlockedTiers) : BASE_COST;
	}

	/** Attempts to buy one map. Nothing is spent unless a map is handed over. */
	public static SkillService.Result buy(ServerPlayer player, SkillTree tree, SkillNode node) {
		TreeProgress progress = SkillService.progress(player, tree);
		if (!progress.owns(node.id())) {
			return SkillService.failFor("mastery.toolmastery.buy.fail.locked", node.displayName());
		}
		int cost = SkillService.master(player) ? 0 : cost(node.id(), progress.unlockedTiers);
		if (XpMath.totalPoints(player) < cost) {
			return SkillService.failFor("mastery.toolmastery.fail.xp", cost, XpMath.totalPoints(player));
		}

		ServerLevel level = player.level();
		boolean undiscoveredOnly = RANK_2.equals(node.id());
		List<Holder<Biome>> candidates = new ArrayList<>();
		for (Holder<Biome> holder : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
			if (undiscoveredOnly && holder.unwrapKey()
					.map(key -> progress.seen.contains("biome/" + key.identifier()))
					.orElse(true)) {
				continue;
			}
			candidates.add(holder);
		}
		if (candidates.isEmpty()) {
			return SkillService.failFor("mastery.toolmastery.buy.fail.all_seen");
		}

		RandomSource random = player.getRandom();
		for (int attempt = 0; attempt < ATTEMPTS && !candidates.isEmpty(); attempt++) {
			Holder<Biome> target = candidates.remove(random.nextInt(candidates.size()));
			ResourceKey<Biome> key = target.unwrapKey().orElse(null);
			if (key == null) {
				continue;
			}
			Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(holder -> holder.is(key),
				player.blockPosition(), LOCATE_RADIUS, LOCATE_STEP_HORIZONTAL, LOCATE_STEP_VERTICAL);
			if (found == null) {
				continue;
			}
			BlockPos pos = found.getFirst();
			Component biomeName = Component.translatable(
				"biome." + key.identifier().getNamespace() + "." + key.identifier().getPath());

			ItemStack map = MapItem.create(level, pos.getX(), pos.getZ(), (byte) 2, true, true);
			MapItem.renderBiomePreviewMap(level, map);
			MapItemSavedData.addTargetDecoration(map, pos, "+", MapDecorationTypes.TARGET_X);
			map.set(DataComponents.ITEM_NAME,
				Component.translatable("item.toolmastery.biome_chart", biomeName));

			if (cost > 0) {
				player.giveExperiencePoints(-cost);
			}
			if (!player.addItem(map)) {
				player.drop(map, false);
			}
			return SkillService.okFor("mastery.toolmastery.buy.ok", biomeName, cost);
		}
		return SkillService.failFor("mastery.toolmastery.buy.fail.not_found");
	}
}
