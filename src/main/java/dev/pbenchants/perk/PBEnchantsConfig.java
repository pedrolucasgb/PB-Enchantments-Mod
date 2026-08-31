package dev.pbenchants.perk;

import dev.pbenchants.PBEnchants;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The two switches a server owner might actually want, and nothing else.
 *
 * <p>The Sword tree is the first part of the mod whose balance depends on what
 * kind of server it is running on. Everything else — mining speed, tree
 * felling, sorting — plays the same in a solo world and on a PvP server. Armour
 * penetration and a 1.8 attack cooldown do not.
 *
 * <ul>
 *   <li>{@code pvp_perks} (default {@code false}) — turn the PvE-only nodes back
 *       on against players. Off means Executioner, Sundering Blow, Bloodthirst,
 *       Adrenaline, Cleave, Gravity Well, Phalanx, Headhunter, Warlord's Wake
 *       and Death Eyes simply do not fire at a player.</li>
 *   <li>{@code nostalgy_pvp} (default {@code true}) — Nostalgy's shortened
 *       attack cooldown works against players, which is the whole point of the
 *       node. A server that disagrees sets this false and the ladder becomes
 *       PvE-only like the rest.</li>
 * </ul>
 *
 * <p>Deliberately hand-parsed rather than pulling in a config library: two
 * booleans do not justify a dependency, and a malformed file falls back to the
 * defaults with a line in the log instead of stopping the game from loading.
 */
public final class PBEnchantsConfig {
	private static final String FILE_NAME = "pbenchants.json";

	private static final String DEFAULT_CONTENT = """
		{
		  "_comment": "pvp_perks: let the PvE-only sword nodes fire at players too. nostalgy_pvp: Nostalgy's cooldown works in PvP.",
		  "pvp_perks": false,
		  "nostalgy_pvp": true
		}
		""";

	private static boolean pvpPerks = false;
	private static boolean nostalgyPvp = true;
	private static boolean loaded = false;

	private PBEnchantsConfig() {
	}

	/** True when the PvE-only set has been switched back on for this server. */
	public static boolean pvpPerks() {
		ensureLoaded();
		return pvpPerks;
	}

	/** True when Nostalgy's shortened cooldown applies against players. */
	public static boolean nostalgyPvp() {
		ensureLoaded();
		return nostalgyPvp;
	}

	/** Reads the file, writing a commented default one the first time. */
	public static synchronized void load() {
		loaded = true;
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			if (!Files.exists(path)) {
				// The mod shipped as Tool Mastery before the rename; a server
				// upgrading in place keeps the settings it already chose.
				Path legacy = FabricLoader.getInstance().getConfigDir().resolve("toolmastery.json");
				if (Files.exists(legacy)) {
					Files.move(legacy, path);
				}
			}
			if (!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				Files.writeString(path, DEFAULT_CONTENT, StandardCharsets.UTF_8);
				return;
			}
			String text = Files.readString(path, StandardCharsets.UTF_8);
			pvpPerks = readFlag(text, "pvp_perks", false);
			nostalgyPvp = readFlag(text, "nostalgy_pvp", true);
		} catch (IOException | RuntimeException failure) {
			PBEnchants.LOGGER.warn("Could not read {} — using defaults ({})", FILE_NAME, failure.toString());
		}
	}

	/**
	 * The client entrypoint and the server entrypoint both reach these getters,
	 * and only one of them calls {@link #load}. Reading before the load is a
	 * defaults answer rather than a crash.
	 */
	private static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	/** {@code "key": true} anywhere in the file, whitespace-tolerant, comments ignored. */
	private static boolean readFlag(String text, String key, boolean fallback) {
		int at = text.indexOf('"' + key + '"');
		if (at < 0) {
			return fallback;
		}
		int colon = text.indexOf(':', at);
		if (colon < 0) {
			return fallback;
		}
		String tail = text.substring(colon + 1).stripLeading();
		if (tail.startsWith("true")) {
			return true;
		}
		if (tail.startsWith("false")) {
			return false;
		}
		return fallback;
	}
}
