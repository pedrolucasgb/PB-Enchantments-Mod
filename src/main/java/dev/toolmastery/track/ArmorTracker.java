package dev.toolmastery.track;

import dev.toolmastery.progress.TreeProgress;
import dev.toolmastery.skill.SkillService;
import dev.toolmastery.skill.SkillTrees;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Feeds the Armor tree's gates. This is the one class that cannot level from
 * <em>doing</em> something — wearing armour is not an action — so every counter
 * here reads damage that happened to the player and what their set did about
 * it.
 *
 * <p>Two hooks carry most of it. {@code AFTER_DAMAGE} hands over the damage
 * before and after mitigation, and the difference between the two <em>is</em>
 * the number the class is scored on. Shield blocking is taken separately, off
 * the mixin on {@code applyItemBlocking}, because a fully blocked hit never
 * reaches the damage event at all — its whole point is that no damage got
 * through.
 */
public final class ArmorTracker {
	/** Fall damage vanilla deals for a five-block drop: {@code floor(distance - 3)}. */
	private static final float FIVE_BLOCK_FALL = 2.0F;

	/** Below this share of max health, a kill counts for the tier 3 gate. */
	private static final float LOW_HEALTH = 0.3F;

	private ArmorTracker() {
	}

	/**
	 * {@code ServerLivingEntityEvents.AFTER_DAMAGE}: base is the damage before
	 * armour and enchantments, taken is what actually landed.
	 *
	 * <p>Absorption only counts while the player is wearing all four pieces, so
	 * the gate reads the way it is written — "absorb 500 damage in a full set" —
	 * and a helmet on its own does not creep the counter forward.
	 */
	public static void onDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		TreeProgress progress = SkillService.progress(player, SkillTrees.ARMOR);

		int absorbed = Math.round(base - taken);
		if (absorbed > 0 && !blocked && wearsFullSet(player)) {
			progress.addCount("absorb_damage", absorbed);
		}
		// Base damage, not applied damage: a fall softened by Feather Falling is
		// still a fall you walked away from, and that is what the gate asks.
		if (source.is(DamageTypes.FALL) && base >= FIVE_BLOCK_FALL && player.isAlive()) {
			progress.addCount("survive_falls", 1);
		}
		if (source.is(DamageTypes.WITHER_SKULL) && player.isAlive()) {
			progress.counters.put("survive_wither_skull", 1);
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

		String material = fullSetMaterial(player);
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
		}
	}

	/** True when all four armour slots hold something, whatever it is. */
	private static boolean wearsFullSet(ServerPlayer player) {
		for (EquipmentSlot slot : new EquipmentSlot[]{
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (player.getItemBySlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** The material of a full matching set, or null for a partial or mixed one. */
	@Nullable
	private static String fullSetMaterial(ServerPlayer player) {
		String material = material(player.getItemBySlot(EquipmentSlot.HEAD));
		if (material == null) {
			return null;
		}
		for (EquipmentSlot slot : new EquipmentSlot[]{
			EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (!material.equals(material(player.getItemBySlot(slot)))) {
				return null;
			}
		}
		return material;
	}

	@Nullable
	private static String material(ItemStack stack) {
		if (stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
			|| stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)) {
			return "leather";
		}
		if (stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.CHAINMAIL_CHESTPLATE)
			|| stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.CHAINMAIL_BOOTS)) {
			return "chainmail";
		}
		if (stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
			|| stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)) {
			return "iron";
		}
		if (stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
			|| stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS)) {
			return "gold";
		}
		if (stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
			|| stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)) {
			return "diamond";
		}
		if (stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
			|| stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)) {
			return "netherite";
		}
		return null;
	}
}
