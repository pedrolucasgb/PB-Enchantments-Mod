package dev.toolmastery.skill;

/**
 * One achievement inside a tier gate. Progress is tracked per player in a
 * counter keyed by {@code id}; the gate line is complete once the counter
 * reaches {@code target}.
 *
 * @param id     counter key, unique within the tree (e.g. "break_stone")
 * @param target count required to complete this line
 */
public record GateRequirement(String id, int target) {
}
