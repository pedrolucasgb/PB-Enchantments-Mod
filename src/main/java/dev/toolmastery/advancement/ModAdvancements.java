package dev.toolmastery.advancement;

import dev.toolmastery.ToolMastery;
import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTree;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mirrors tier unlocks onto vanilla advancements so every tier shows up in the
 * advancements screen ("L"). The data pack entries live in
 * {@code data/toolmastery/advancement/<tree>/tier_<n>.json} and use the
 * impossible trigger — they are only ever awarded from here.
 */
public final class ModAdvancements {
	/** Criterion name shared by every tier advancement. */
	private static final String CRITERION = "unlocked";

	private ModAdvancements() {
	}

	/** Advancement id for a 0-based tier index: tier 0 of "pickaxe" → {@code toolmastery:pickaxe/tier_1}. */
	public static Identifier tierId(String treeId, int tierIndex) {
		return Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, treeId + "/tier_" + (tierIndex + 1));
	}

	/** Awards the advancement for one tier. Silent no-op when it is already earned. */
	public static void grantTier(ServerPlayer player, String treeId, int tierIndex) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		Identifier id = tierId(treeId, tierIndex);
		AdvancementHolder advancement = server.getAdvancements().get(id);
		if (advancement == null) {
			ToolMastery.LOGGER.warn("Missing tier advancement '{}' — is the mod's data pack loaded?", id);
			return;
		}
		player.getAdvancements().award(advancement, CRITERION);
	}

	/**
	 * Awards every tier this player has already unlocked. Runs on join so saves
	 * made before this feature — and anything cleared with /advancement revoke —
	 * catch up, and after debug grants that move several tiers at once.
	 */
	public static void syncAll(ServerPlayer player) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = SkillService.progress(player, tree);
			for (int tierIndex = 0; tierIndex < progress.unlockedTiers; tierIndex++) {
				grantTier(player, tree.id(), tierIndex);
			}
		}
	}
}
