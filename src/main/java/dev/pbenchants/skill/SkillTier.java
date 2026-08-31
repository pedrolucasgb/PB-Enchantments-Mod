package dev.pbenchants.skill;

import java.util.List;

/**
 * A tier of a skill tree. Unlocking requires every gate line complete plus
 * paying {@code accessCost} — declared in levels, charged as that level's
 * fixed point equivalent via {@link XpMath}.
 */
public record SkillTier(int accessCost, List<GateRequirement> gates) {
}
