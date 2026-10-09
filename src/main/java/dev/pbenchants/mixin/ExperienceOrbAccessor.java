package dev.pbenchants.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The three private bits of an orb {@code perk.XpOrbs} folds experience into. */
@Mixin(ExperienceOrb.class)
public interface ExperienceOrbAccessor {
	@Accessor("count")
	int pbenchants$count();

	@Accessor("age")
	void pbenchants$setAge(int age);

	// 1.21.1 has no setValue(): write the field directly.
	@Accessor("value")
	void pbenchants$setValue(int value);
}
