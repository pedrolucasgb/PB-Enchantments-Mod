package dev.pbenchants.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Rapid Reload II loads a stowed crossbow exactly the way a held reload does —
 * by calling the same {@code draw} vanilla calls, Multishot copies, Infinity
 * rules, creative handling and all. The method is protected static, hence the
 * invoker rather than a re-implementation that would drift.
 */
@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponItemInvoker {
	@Invoker("draw")
	static List<ItemStack> pbenchants$draw(ItemStack weapon, ItemStack ammo, LivingEntity shooter) {
		throw new AssertionError("mixin invoker not applied");
	}
}
