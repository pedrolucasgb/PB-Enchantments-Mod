package dev.pbenchants.track;

import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Feeds the Armor tree's gates. This is the one class that cannot level from
 * <em>doing</em> something — wearing armour is not an action — so every counter
 * here reads damage that happened to the player and what their set did about
 * it.
 *
 * <p>Three hooks carry most of it. {@code ArmorPlayerMixin} measures what the
 * armour actually soaked, inside {@code actuallyHurt} where vanilla does the
 * soaking — the one place that number exists. {@code AFTER_DAMAGE} carries the
 * survive-this gates, which only need to know a hit happened and how big it
 * started. Shield blocking is taken separately, off the mixin on
 * {@code applyItemBlocking}, because a fully blocked hit never reaches the
 * damage event at all — its whole point is that no damage got through.
 */
public final class ArmorTracker {
	/** Fall damage vanilla deals for a five-block drop: {@code floor(distance - 3)}. */
	private static final float FIVE_BLOCK_FALL = 2.0F;

	/** Below this share of max health, a kill counts for the tier 3 gate. */
	private static final float LOW_HEALTH = 0.3F;

	private ArmorTracker() {
	}

	/**
	 * {@code ServerLivingEntityEvents.AFTER_DAMAGE}: the survive-this gates.
	 * base is the damage on the way into {@code hurtServer}, taken is the same
	 * amount after the shield's share.
	 *
	 * <p>The absorb gate does <b>not</b> live here any more. It looked like it
	 * could — "the damage the armour absorbed is base minus taken" — but the
	 * event fires before armour is applied: {@code actuallyHurt} does that
	 * reduction on a local of its own, so base and taken only ever differ by
	 * what a shield blocked. The real number is read in
	 * {@code ArmorPlayerMixin} and lands in {@link #onArmorAbsorb}.
	 */
	public static void onDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);

		// Base damage, not applied damage: a fall softened by Feather Falling is
		// still a fall you walked away from, and that is what the gate asks.
		if (source.is(DamageTypes.FALL) && base >= FIVE_BLOCK_FALL && player.isAlive()) {
			progress.addCount("survive_falls", 1);
		}
		if (source.is(DamageTypes.WITHER_SKULL) && player.isAlive()) {
			progress.counters.put("survive_wither_skull", 1);
		}
		if (source.is(DamageTypeTags.IS_EXPLOSION) && player.isAlive()) {
			progress.addCount("survive_explosions", 1);
		}
	}

	/**
	 * What the armour, Protection and Resistance soaked out of one hit —
	 * measured in {@code ArmorPlayerMixin} across vanilla's own two reduction
	 * calls, which is the only place that difference exists as a number.
	 *
	 * <p>Only counts while the player is wearing all four pieces, so the gate
	 * reads the way it is written — "absorb 500 damage in a full set" — and a
	 * helmet on its own does not creep the counter forward.
	 */
	public static void onArmorAbsorb(ServerPlayer player, float absorbed) {
		int amount = Math.round(absorbed);
		if (amount > 0 && ArmorPerks.wearsFullSet(player)) {
			SkillService.progress(player, SkillTrees.ARMOR).addCount("absorb_damage", amount);
		}
	}

	/**
	 * One hit stopped by a raised shield, with the amount the shield soaked.
	 * Called from {@code LivingEntityMixin} on the defender's side.
	 */
	public static void onShieldBlock(ServerPlayer player, float blocked) {
		if (blocked <= 0.0F) {
			return;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);
		progress.addCount("block_damage", Math.round(blocked));
		progress.addCount("block_hits", 1);
	}

	/** A mob died to this player: the gate only wants the ones killed while hurt. */
	public static void onMobKill(ServerPlayer player) {
		if (player.getHealth() < player.getMaxHealth() * LOW_HEALTH) {
			SkillService.addCount(player, SkillTrees.ARMOR, "low_health_kills", 1);
		}
	}

	/**
	 * A crafted result on its way out of the grid. The gate wants a full iron
	 * set, so this is a checklist of the four distinct pieces — crafting four
	 * helmets is not a set.
	 */
	public static void onCraft(ServerPlayer player, ItemStack stack) {
		String piece = null;
		if (stack.is(Items.IRON_HELMET)) {
			piece = "helmet";
		} else if (stack.is(Items.IRON_CHESTPLATE)) {
			piece = "chestplate";
		} else if (stack.is(Items.IRON_LEGGINGS)) {
			piece = "leggings";
		} else if (stack.is(Items.IRON_BOOTS)) {
			piece = "boots";
		}
		if (piece != null) {
			SkillService.progress(player, SkillTrees.ARMOR).see("iron_armor", piece, "craft_iron_armor");
		}
	}

	/** Called once a second per online player: everything measured in time worn. */
	public static void tick(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);

		if (player.isOnFire() || player.isInLava()) {
			progress.addCount("survive_fire_seconds", 1);
		}

		String material = ArmorPerks.fullSetMaterial(player);
		if (material == null) {
			return;
		}
		progress.see("armor_set", material, "armor_checklist");
		if (material.equals("diamond")) {
			// Held in seconds and shown in minutes: an in-game day is twenty
			// real minutes, so the gate line reads as a number a player can
			// watch move rather than as 1200 of something.
			int seconds = progress.count("wear_diamond_seconds") + 1;
			progress.counters.put("wear_diamond_seconds", seconds);
			progress.counters.put("wear_diamond_minutes", seconds / 60);
		} else if (material.equals("netherite")) {
			// The gate says "own"; wearing it is the version we can see, and
			// nobody owns a netherite set they never put on.
			progress.counters.put("own_netherite_set", 1);
			int seconds = progress.count("wear_netherite_seconds") + 1;
			progress.counters.put("wear_netherite_seconds", seconds);
			progress.counters.put("wear_netherite_minutes", seconds / 60);
		}
	}

}
