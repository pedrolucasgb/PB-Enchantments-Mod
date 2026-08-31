package dev.pbenchants.skill;

/**
 * One achievement inside a tier gate. Progress is tracked per player in a
 * counter keyed by {@code id}; the gate line is complete once the counter
 * reaches {@code target}.
 *
 * @param id     counter key, unique within the tree (e.g. "break_stone")
 * @param target count required to complete this line
 */
public record GateRequirement(String id, int target) {
	/** Human-readable name derived from the id: "chop_logs_total" → "Chop Logs Total". */
	public String displayName() {
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
