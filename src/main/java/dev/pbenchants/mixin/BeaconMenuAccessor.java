package dev.pbenchants.mixin;

import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where the beacon behind an open menu stands — the tracker keys "distinct beacons" on it. */
@Mixin(BeaconMenu.class)
public interface BeaconMenuAccessor {
	@Accessor("access")
	ContainerLevelAccess pbenchants$access();
}
