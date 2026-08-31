package dev.pbenchants.perk;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Duck interface {@code AbstractArrowMixin} adds to every arrow: the point it
 * was loosed from. Distance-scaled nodes measure the <em>shot</em>, and the
 * shooter's position at impact is not the shot — a backpedalling archer would
 * farm the distance gates, and a charging one would be robbed.
 *
 * <p>Null for an arrow that was already in flight when the server started;
 * callers fall back on the shooter's position, which is wrong by at most one
 * fight.
 */
public interface ArrowOrigin {
	@Nullable
	Vec3 pbenchants$origin();
}
