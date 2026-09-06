package dev.pbenchants.client;

import dev.pbenchants.PBEnchants;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The handful of choices that belong to a player's screen rather than to a
 * world, kept in {@code config/pbenchants-client.json} next to the server-side
 * {@code pbenchants.json}.
 *
 * <p>Two so far: whether the skill screen paints itself solid or lets the
 * world show through, and whether Night Eyes is switched on. Both are a
 * matter of taste (or of wanting to see a cave the way it really is for a
 * screenshot) rather than of balance, so neither is synced nor validated by
 * the server — Night Eyes is a picture on this client and nothing else.
 *
 * <p>Hand-parsed for the same reason the server config is: two booleans do
 * not justify a JSON dependency, and an unreadable file falls back to the
 * defaults with a line in the log rather than taking the game down.
 */
public final class ClientSettings {
	private static final String FILE_NAME = "pbenchants-client.json";

	private static boolean translucentTree = false;
	private static boolean nightEyes = true;
	private static boolean loaded = false;

	private ClientSettings() {
	}

	/** True when the skill screen should let the world show through. */
	public static boolean translucentTree() {
		ensureLoaded();
		return translucentTree;
	}

	/** Flips the backdrop and writes the choice straight back to disk. */
	public static void toggleTranslucentTree() {
		ensureLoaded();
		translucentTree = !translucentTree;
		save();
	}

	/**
	 * Whether an owned Night Eyes is currently drawing. On by default — the
	 * node was bought to see in the dark — and the toggle key flips it for
	 * the moments when real darkness is wanted.
	 */
	public static boolean nightEyes() {
		ensureLoaded();
		return nightEyes;
	}

	/** Flips Night Eyes and writes the choice straight back to disk. Returns the new state. */
	public static boolean toggleNightEyes() {
		ensureLoaded();
		nightEyes = !nightEyes;
		save();
		return nightEyes;
	}

	private static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	private static synchronized void load() {
		loaded = true;
		Path path = path();
		if (!Files.exists(path)) {
			return;
		}
		try {
			String text = Files.readString(path, StandardCharsets.UTF_8);
			translucentTree = readFlag(text, "translucent_tree", false);
			nightEyes = readFlag(text, "night_eyes", true);
		} catch (IOException | RuntimeException failure) {
			PBEnchants.LOGGER.warn("Could not read {} — using defaults ({})", FILE_NAME, failure.toString());
		}
	}

	private static synchronized void save() {
		Path path = path();
		String content = """
			{
			  "_comment": "translucent_tree: the skill screen lets the world show through instead of painting a solid backdrop. night_eyes: whether an owned Night Eyes is switched on (the toggle key flips it).",
			  "translucent_tree": %s,
			  "night_eyes": %s
			}
			""".formatted(translucentTree, nightEyes);
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, content, StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException failure) {
			PBEnchants.LOGGER.warn("Could not write {} ({})", FILE_NAME, failure.toString());
		}
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** {@code "key": true} anywhere in the file, whitespace-tolerant. */
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
