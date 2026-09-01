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
 * <p>Only one so far: whether the skill screen paints itself solid or lets the
 * world show through, which is a matter of taste (and of wanting to keep an eye
 * on the cave you are standing in) rather than of balance, so it is neither
 * synced nor validated by the server.
 *
 * <p>Hand-parsed for the same reason the server config is: one boolean does not
 * justify a JSON dependency, and an unreadable file falls back to the default
 * with a line in the log rather than taking the game down.
 */
public final class ClientSettings {
	private static final String FILE_NAME = "pbenchants-client.json";

	private static boolean translucentTree = false;
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
			translucentTree = readFlag(Files.readString(path, StandardCharsets.UTF_8), "translucent_tree", false);
		} catch (IOException | RuntimeException failure) {
			PBEnchants.LOGGER.warn("Could not read {} — using defaults ({})", FILE_NAME, failure.toString());
		}
	}

	private static synchronized void save() {
		Path path = path();
		String content = """
			{
			  "_comment": "translucent_tree: the skill screen lets the world show through instead of painting a solid backdrop.",
			  "translucent_tree": %s
			}
			""".formatted(translucentTree);
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
