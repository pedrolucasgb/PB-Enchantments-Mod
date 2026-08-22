package dev.toolmastery.skill;

import java.util.List;

/**
 * A tier of a skill tree. Unlocking requires every gate line complete plus
 * paying {@code accessCost} XP levels.
 */
public record SkillTier(int accessCost, List<GateRequirement> gates) {
}
