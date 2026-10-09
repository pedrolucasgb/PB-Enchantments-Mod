package dev.pbenchants;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility testing only ({@code -Dpbenchants.mixinAudit=true}): force-loads
 * every class PB mixes into, so an injector that cannot apply — because the
 * target changed, or because another mod's mixin got there first — fails at
 * boot instead of deep in play. Class literals are used on purpose: the build
 * remaps them, so the probe works in production (intermediary names) as well
 * as in the dev runs.
 *
 * <p>After PB's own targets it also asks Mixin to audit every other mod's
 * targets; that pass is best-effort, because some mods declare common mixins
 * into client-only classes and the audit stops at the first class a server
 * cannot load.
 */
public final class MixinProbe {
	private MixinProbe() {
	}

	/** PB's common (both sides) mixin targets. */
	public static List<Class<?>> commonTargets() {
		List<Class<?>> targets = new ArrayList<>(List.of(
			net.minecraft.world.inventory.AbstractContainerMenu.class,
			net.minecraft.world.inventory.AnvilMenu.class,
			net.minecraft.world.item.AxeItem.class,
			net.minecraft.world.level.block.entity.BeaconBlockEntity.class,
			net.minecraft.world.inventory.BeaconMenu.class,
			net.minecraft.world.level.block.Block.class,
			net.minecraft.world.item.BlockItem.class,
			net.minecraft.world.entity.vehicle.Boat.class,
			net.minecraft.world.item.BoneMealItem.class,
			net.minecraft.world.inventory.CraftingMenu.class,
			net.minecraft.world.inventory.EnchantmentMenu.class,
			net.minecraft.world.entity.ExperienceOrb.class,
			net.minecraft.world.entity.projectile.FireworkRocketEntity.class,
			net.minecraft.world.item.FireworkRocketItem.class,
			net.minecraft.world.inventory.FurnaceResultSlot.class,
			net.minecraft.world.inventory.GrindstoneMenu.class,
			net.minecraft.world.item.HoeItem.class,
			net.minecraft.world.entity.player.Inventory.class,
			net.minecraft.world.entity.item.ItemEntity.class,
			net.minecraft.world.item.ItemStack.class,
			net.minecraft.world.entity.LivingEntity.class,
			net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction.class,
			net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction.class,
			net.minecraft.world.inventory.MerchantResultSlot.class,
			net.minecraft.world.entity.player.Player.class,
			net.minecraft.world.item.ProjectileWeaponItem.class,
			net.minecraft.world.inventory.ResultSlot.class,
			net.minecraft.server.network.ServerGamePacketListenerImpl.class,
			net.minecraft.server.level.ServerPlayer.class,
			net.minecraft.world.inventory.Slot.class
		));
		return targets;
	}

	/** Loads (and initialises) each class plus GrindstoneMenu's result slot; logs one line per class. */
	public static int probe(List<Class<?>> targets, String side) {
		ClassLoader loader = MixinProbe.class.getClassLoader();
		List<String> names = new ArrayList<>();
		for (Class<?> target : targets) {
			names.add(target.getName());
		}
		names.add(net.minecraft.world.inventory.GrindstoneMenu.class.getName() + "$4");
		int failures = 0;
		for (String name : names) {
			try {
				Class.forName(name, true, loader);
				PBEnchants.LOGGER.info("PB mixin probe [{}] OK     {}", side, name);
			} catch (Throwable failure) {
				failures++;
				PBEnchants.LOGGER.error("PB mixin probe [{}] FAILED {}: {}", side, name, failure.toString());
			}
		}
		PBEnchants.LOGGER.info("PB mixin probe [{}]: {} targets, {} failures", side, names.size(), failures);
		return failures;
	}

	/** Mixin's own audit over every mod's targets; best-effort, see the class comment. */
	public static void auditEverything(String side) {
		try {
			org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
			PBEnchants.LOGGER.info("PB mixin audit [{}]: every mod's mixin targets loaded without error", side);
		} catch (Throwable failure) {
			PBEnchants.LOGGER.warn("PB mixin audit [{}]: stopped early (not necessarily a PB problem): {}", side,
				failure.toString());
		}
	}
}
