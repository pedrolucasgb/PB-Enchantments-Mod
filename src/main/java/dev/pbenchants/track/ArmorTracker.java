package dev.pbenchants.track;

import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.GateChecklists;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

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

	/** The two armour checklists, in the bit order {@link GateChecklists} declares. */
	private static final List<String> IRON_PIECES = List.of("helmet", "chestplate", "leggings", "boots");
	private static final List<String> MATERIALS =
		List.of("leather", "chainmail", "iron", "gold", "diamond", "netherite");

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
		int bit = -1;
		if (stack.is(Items.IRON_HELMET)) {
			bit = 0;
		} else if (stack.is(Items.IRON_CHESTPLATE)) {
			bit = 1;
		} else if (stack.is(Items.IRON_LEGGINGS)) {
			bit = 2;
		} else if (stack.is(Items.IRON_BOOTS)) {
			bit = 3;
		}
		if (bit >= 0) {
			TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);
			migrate(progress, "craft_iron_armor", "iron_armor/", IRON_PIECES);
			GateChecklists.tick(progress, "craft_iron_armor", bit);
		}
	}

	/**
	 * All four pieces on, and every one of them trimmed. Pattern and material
	 * are the wearer's business — the gate asks that the set was decorated, not
	 * that it matches.
	 */
	private static boolean wearsTrimmedSet(ServerPlayer player) {
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (piece.isEmpty() || !piece.has(DataComponents.TRIM)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Both armour checklists were name lists before they were bitmasks, so a
	 * save written before the switch has its entries in {@code seen} and no
	 * mask at all. Seeding the mask from those names once, the first time
	 * either gate is touched, is what keeps a player's crafted set and worn
	 * materials from resetting to zero under the new counter.
	 */
	private static void migrate(TreeProgress progress, String gateId, String prefix, List<String> roster) {
		if (progress.counters.containsKey(GateChecklists.maskId(gateId))) {
			return;
		}
		int mask = 0;
		for (String entry : progress.seen) {
			if (entry.startsWith(prefix)) {
				int bit = roster.indexOf(entry.substring(prefix.length()));
				if (bit >= 0) {
					mask |= 1 << bit;
				}
			}
		}
		progress.counters.put(GateChecklists.maskId(gateId), mask);
		progress.counters.put(gateId, Integer.bitCount(mask));
	}

	/** Called once a second per online player: everything measured in time worn. */
	public static void tick(ServerPlayer player) {
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);

		if (player.isOnFire() || player.isInLava()) {
			progress.addCount("survive_fire_seconds", 1);
		}

		if (wearsTrimmedSet(player)) {
			progress.counters.put("trim_full_set", 1);
		}

		String material = ArmorPerks.fullSetMaterial(player);
		if (material == null) {
			return;
		}
		int materialBit = MATERIALS.indexOf(material);
		if (materialBit >= 0) {
			migrate(progress, "armor_checklist", "armor_set/", MATERIALS);
			GateChecklists.tick(progress, "armor_checklist", materialBit);
		}
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
