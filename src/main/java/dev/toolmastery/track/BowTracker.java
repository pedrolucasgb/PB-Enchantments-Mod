package dev.toolmastery.track;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/**
 * Feeds the gates of the Bow tree. The class's whole identity is distance, so
 * its trackers are the four facts the issue asked for and vanilla never
 * recorded: arrows fired vs. landed, how far the shot was when it hit, whether
 * the target was in the air, and what kind of arrow left the string.
 *
 * <p>Everything hangs off two moments — the shot leaving (one hook inside
 * {@code ProjectileWeaponItem.shoot}, which both the bow and the crossbow
 * funnel through) and the arrow arriving (the damage hook, and the kill event).
 * The shot distance is measured from where the <em>arrow</em> was loosed, not
 * where the shooter stands at impact, so backpedalling does not farm the
 * distance gates.
 *
 * <p>Counters are <b>cumulative</b>, like every other tree in the mod.
 */
public final class BowTracker {
	/** Kills at or past these marks feed the two distance gates. */
	private static final double LONG_KILL = 30.0;
	private static final double VERY_LONG_KILL = 60.0;

	private BowTracker() {
	}

	private static TreeProgress progress(ServerPlayer player) {
		return SkillService.progress(player, SkillTrees.BOW);
	}

	/**
	 * Arrows left the string — one count per projectile, so a Multishot volley
	 * is honestly three. Tipped arrows also feed the potion checklist, by the
	 * potion actually on the arrow.
	 */
	public static void onArrowsFired(ServerPlayer player, ItemStack weapon, List<ItemStack> projectiles) {
		TreeProgress progress = progress(player);
		progress.addCount("arrows_fired", projectiles.size());
		for (ItemStack projectile : projectiles) {
			if (!projectile.is(Items.TIPPED_ARROW)) {
				continue;
			}
			PotionContents contents = projectile.get(DataComponents.POTION_CONTENTS);
			if (contents == null) {
				continue;
			}
			String id = contents.potion()
				.flatMap(Holder::unwrapKey)
				.map(key -> key.identifier().toString())
				.orElse(contents.customEffects().isEmpty() ? null : "custom");
			if (id != null) {
				progress.see("tipped", id, "tipped_checklist");
			}
		}
	}

	/** An arrow connected with something. The gate counts the hit, not the harm. */
	public static void onArrowHit(ServerPlayer player) {
		progress(player).addCount("arrows_hit", 1);
	}

	/**
	 * An arrow kill. Hostile-only for the counters, same rule as the Sword
	 * tree: a class that levelled off penned chickens would pace itself in a
	 * pen. The phantom line is the exception by construction — phantoms are
	 * hostile anyway, and the gate wants them still airborne.
	 *
	 * @param origin where the killing arrow was loosed, or null for an arrow
	 *               older than the server session
	 */
	public static void onKill(ServerPlayer player, LivingEntity victim, AbstractArrow arrow,
			net.minecraft.world.phys.Vec3 origin) {
		if (!(victim instanceof Enemy)) {
			return;
		}
		TreeProgress progress = progress(player);
		progress.addCount("ranged_kills", 1);

		ItemStack weapon = arrow.getWeaponItem();
		if (weapon != null && weapon.is(Items.BOW)) {
			progress.addCount("bow_kills", 1);
		}
		if (weapon != null && weapon.is(Items.CROSSBOW)) {
			progress.addCount("crossbow_kills", 1);
			if (vanillaLevel(player, weapon, Enchantments.MULTISHOT) > 0) {
				progress.addCount("multishot_kills", 1);
			}
		}

		double distance = (origin != null ? origin : player.position()).distanceTo(victim.position());
		if (distance >= LONG_KILL) {
			progress.addCount("kills_30", 1);
		}
		if (distance >= VERY_LONG_KILL) {
			progress.addCount("kills_60", 1);
		}
		if (victim.getType() == EntityTypes.PHANTOM && !victim.onGround()) {
			progress.addCount("phantom_air_kills", 1);
		}
	}

	private static int vanillaLevel(ServerPlayer player, ItemStack stack,
			net.minecraft.resources.ResourceKey<Enchantment> key) {
		Holder<Enchantment> holder = player.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(key)
			.orElse(null);
		return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}
}
