package dev.pbenchants.skill;

import net.minecraft.network.chat.Component;

/**
 * One achievement inside a tier gate. Progress is tracked per player in a
 * counter keyed by {@code id}; the gate line is complete once the counter
 * reaches {@code target}.
 *
 * @param id     counter key, unique within the tree (e.g. "break_stone")
 * @param target count required to complete this line
 */
public record GateRequirement(String id, int target) {
	/**
	 * Human-readable name, localised: resolved through the lang file as
	 * {@code gate.pbenchants.<id>} so the gate lists follow the client's
	 * language like every other string. A gate someone forgot to give a lang
	 * entry falls back to the name derived from its id ("chop_logs_total" →
	 * "Chop Logs Total") — readable English rather than a raw key on screen.
	 *
	 * <p>Resolved to a string at the call site because every consumer builds
	 * plain text around it (checkbox glyphs, "3/10" counts). On the server —
	 * {@code /mastery status} — that resolution is en_us, which is the
	 * console's language anyway.
	 */
	public String displayName() {
		return Component.translatableWithFallback("gate.pbenchants." + id, derivedName()).getString();
	}

	/** The id title-cased, the pre-lang name and the fallback for a missing entry. */
	private String derivedName() {
		StringBuilder sb = new StringBuilder(id.length());
		for (String word : id.split("_")) {
			if (word.isEmpty()) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
		}
		return sb.toString();
	}
}
