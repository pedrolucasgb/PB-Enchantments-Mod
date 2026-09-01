package dev.pbenchants.advancement;

import dev.pbenchants.PBEnchants;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTree;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Mirrors tier unlocks onto vanilla advancements so every tier shows up in the
 * advancements screen ("L"). The data pack entries live in
 * {@code data/PBEnchants/advancement/<tree>/tier_<n>.json} and use the
 * impossible trigger — they are only ever awarded from here.
 *
 * <p>The root of that tab is the mod's own first step: opening the skill
 * screen. It used to tick itself in on the first server tick, which put a tree
 * of locked advancements in front of a player who had no idea the mod was
 * there; now the first press of the tree key earns it, and the join hint in
 * chat points at that key.
 */
public final class ModAdvancements {
	/** Criterion name shared by the root and every tier advancement. */
	private static final String CRITERION = "unlocked";

	/** The tab's root: earned by opening the skill screen for the first time. */
	private static final Identifier ROOT =
		Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, "root");

	private ModAdvancements() {
	}

	/** Advancement id for a 0-based tier index: tier 0 of "pickaxe" → {@code toolmastery:pickaxe/tier_1}. */
	public static Identifier tierId(String treeId, int tierIndex) {
		return Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, treeId + "/tier_" + (tierIndex + 1));
	}

	/**
	 * Awards the root advancement — the mod's "you found me" step. Called every
	 * time the skill screen asks the server for a snapshot, which is every time
	 * it opens; vanilla ignores an award the player already holds, so there is
	 * nothing to guard against here.
	 */
	public static void grantRoot(ServerPlayer player) {
		award(player, ROOT);
	}

	/** Awards the advancement for one tier. Silent no-op when it is already earned. */
	public static void grantTier(ServerPlayer player, String treeId, int tierIndex) {
		award(player, tierId(treeId, tierIndex));
	}

	private static void award(ServerPlayer player, Identifier id) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder advancement = server.getAdvancements().get(id);
		if (advancement == null) {
			PBEnchants.LOGGER.warn("Missing advancement '{}' — is the mod's data pack loaded?", id);
			return;
		}
		player.getAdvancements().award(advancement, CRITERION);
	}

	/** Takes the advancement for one tier back. Silent no-op when it is not held. */
	public static void revokeTier(ServerPlayer player, String treeId, int tierIndex) {
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}
		AdvancementHolder advancement = server.getAdvancements().get(tierId(treeId, tierIndex));
		if (advancement != null) {
			player.getAdvancements().revoke(advancement, CRITERION);
		}
	}

	/**
	 * Makes the advancements screen match the skill trees exactly: every
	 * unlocked tier awarded, every locked one taken back. Runs on join so saves
	 * made before this feature — and anything cleared with /advancement revoke —
	 * catch up, after debug grants that move several tiers at once, and after a
	 * debug reset, which is the reason this also revokes.
	 */
	public static void syncAll(ServerPlayer player) {
		for (SkillTree tree : SkillTrees.ALL.values()) {
			TreeProgress progress = SkillService.progress(player, tree);
			for (int tierIndex = 0; tierIndex < tree.tiers().size(); tierIndex++) {
				if (tierIndex < progress.unlockedTiers) {
					grantTier(player, tree.id(), tierIndex);
				} else {
					revokeTier(player, tree.id(), tierIndex);
				}
			}
		}
	}
}
