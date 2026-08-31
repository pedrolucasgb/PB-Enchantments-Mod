package dev.pbenchants.perk;

import dev.pbenchants.PBEnchants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Armor nodes that are not answers to a hit but a state the player is in:
 * Sure Footing's attributes, Last Stand's rescue, Repair Rites' slow mend,
 * Nightplate's set bonus and Second Skin's warning. Plus the two capstones that
 * hang off an event rather than a mixin.
 *
 * <p>All of it is transient. A relog re-applies the attributes on the next tick
 * and starts the calm timer over, which is the honest answer anyway — you were
 * not standing still, you were gone.
 */
public final class ArmorUpkeep {
	/** Health below which Last Stand fires: four hearts. */
	private static final float LAST_STAND_THRESHOLD = 8.0F;

	/** How long Last Stand's Resistance lasts, and how long before it can fire again. */
	private static final int LAST_STAND_DURATION = 100;
	private static final int LAST_STAND_COOLDOWN = 600;

	/** Ticks of standing still and out of combat before Repair Rites starts mending. */
	private static final int REPAIR_RITES_CALM = 200;

	/** How far a player may drift and still count as standing still. */
	private static final double STILL_EPSILON = 0.01;

	/** Immortal Line's cooldown: ten minutes. */
	private static final int IMMORTAL_LINE_COOLDOWN = 12000;

	/** Durability Living Armor returns per point of experience, matching Mending. */
	private static final int LIVING_ARMOR_PER_POINT = 2;

	private static final Identifier SURE_FOOTING_LAND =
		Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "sure_footing_land");
	private static final Identifier SURE_FOOTING_WATER =
		Identifier.fromNamespaceAndPath(PBEnchants.MOD_ID, "sure_footing_water");

	/** Half of what the enchantments give, which is what the node promises. */
	private static final double SURE_FOOTING_BONUS = 0.5;

	private static final class State {
		int lastStandReadyAt;
		int immortalReadyAt;
		int calmSince = Integer.MAX_VALUE;
		double x;
		double y;
		double z;
		boolean warnedOnSpentPiece;
	}

	private static final Map<UUID, State> states = new HashMap<>();

	private ArmorUpkeep() {
	}

	private static State state(ServerPlayer player) {
		return states.computeIfAbsent(player.getUUID(), uuid -> new State());
	}

	public static void forget(ServerPlayer player) {
		states.remove(player.getUUID());
	}

	/** Any damage at all ends the calm Repair Rites needs. */
	public static void onDamaged(ServerPlayer player) {
		state(player).calmSince = Integer.MAX_VALUE;
	}

	/** Called every server tick per online player. */
	public static void tick(ServerPlayer player) {
		State state = state(player);
		sureFooting(player);
		lastStand(player, state);
		trackStillness(player, state);
	}

	/** Called once a second per online player: the slow, cheap half. */
	public static void slowTick(ServerPlayer player) {
		State state = state(player);
		repairRites(player, state);
		nightplate(player);
		secondSkinWarning(player, state);
	}

	// ---------- Sure Footing ----------

	/**
	 * Depth Strider and Soul Speed are attributes now, not special cases in the
	 * movement code, so half-strength versions of both are two modifiers — and
	 * they stack with the real enchantments instead of fighting them for the
	 * same slot. The soul sand and honey slowdown is the other half of the
	 * node and lives in the entity mixin, where the block speed factor is read.
	 */
	private static void sureFooting(ServerPlayer player) {
		boolean wanted = ArmorPerks.owns(player, ArmorPerks.SURE_FOOTING);
		modifier(player.getAttribute(Attributes.MOVEMENT_EFFICIENCY), SURE_FOOTING_LAND, wanted);
		modifier(player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY), SURE_FOOTING_WATER, wanted);
	}

	private static void modifier(AttributeInstance attribute, Identifier id, boolean wanted) {
		if (attribute == null) {
			return;
		}
		boolean present = attribute.getModifier(id) != null;
		if (wanted == present) {
			return;
		}
		if (wanted) {
			attribute.addTransientModifier(
				new AttributeModifier(id, SURE_FOOTING_BONUS, AttributeModifier.Operation.ADD_VALUE));
		} else {
			attribute.removeModifier(id);
		}
	}

	// ---------- Last Stand ----------

	private static void lastStand(ServerPlayer player, State state) {
		if (player.tickCount < state.lastStandReadyAt
			|| player.getHealth() > LAST_STAND_THRESHOLD
			|| !player.isAlive()
			|| !ArmorPerks.owns(player, ArmorPerks.LAST_STAND)) {
			return;
		}
		state.lastStandReadyAt = player.tickCount + LAST_STAND_COOLDOWN;
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LAST_STAND_DURATION, 0, false, true, true));
		player.sendSystemMessage(
			Component.translatable("perk.pbenchants.last_stand.hold").withStyle(ChatFormatting.GOLD), true);
	}

	// ---------- Repair Rites ----------

	private static void trackStillness(ServerPlayer player, State state) {
		boolean moved = Math.abs(player.getX() - state.x) > STILL_EPSILON
			|| Math.abs(player.getY() - state.y) > STILL_EPSILON
			|| Math.abs(player.getZ() - state.z) > STILL_EPSILON;
		state.x = player.getX();
		state.y = player.getY();
		state.z = player.getZ();
		if (moved) {
			state.calmSince = Integer.MAX_VALUE;
		} else if (state.calmSince == Integer.MAX_VALUE) {
			state.calmSince = player.tickCount;
		}
	}

	/**
	 * A point a second, per piece, once the player has been still and unhurt
	 * for ten seconds. Deliberately slow enough that it is a camp-fire mend and
	 * not a reason to skip the anvil.
	 */
	private static void repairRites(ServerPlayer player, State state) {
		if (!ArmorPerks.owns(player, ArmorPerks.REPAIR_RITES)
			|| player.tickCount - state.calmSince < REPAIR_RITES_CALM) {
			return;
		}
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (piece.isDamageableItem() && piece.getDamageValue() > 0) {
				piece.setDamageValue(piece.getDamageValue() - 1);
			}
		}
	}

	// ---------- Nightplate ----------

	/**
	 * The set bonuses that need upkeep. Gold's piglin neutrality is a mixin on
	 * the anger call instead, and netherite's is vanilla's own knockback
	 * resistance — this is the leather half, which vanilla only ever gave to
	 * the boots.
	 */
	private static void nightplate(ServerPlayer player) {
		if ("leather".equals(ArmorPerks.nightplateMaterial(player)) && player.getTicksFrozen() > 0) {
			player.setTicksFrozen(0);
		}
	}

	// ---------- Second Skin ----------

	private static void secondSkinWarning(ServerPlayer player, State state) {
		if (!ArmorPerks.owns(player, ArmorPerks.SECOND_SKIN)) {
			return;
		}
		ItemStack spent = null;
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (!piece.isEmpty() && ArmorPerks.nearlySpent(piece)) {
				spent = piece;
				break;
			}
		}
		if (spent == null) {
			state.warnedOnSpentPiece = false;
			return;
		}
		if (!state.warnedOnSpentPiece) {
			state.warnedOnSpentPiece = true;
			player.sendSystemMessage(Component.translatable("perk.pbenchants.second_skin.warn",
				spent.getHoverName()).withStyle(ChatFormatting.RED), true);
		}
	}

	// ---------- Immortal Line ----------

	/**
	 * {@code ServerLivingEntityEvents.ALLOW_DEATH}: returning false calls the
	 * death off, and the player is put back on one heart. No totem is spent —
	 * that is the whole point of the capstone — and the ten-minute cooldown is
	 * what stops it being one.
	 */
	public static boolean allowDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source,
			float damage) {
		if (!(entity instanceof ServerPlayer player) || !ArmorPerks.owns(player, ArmorPerks.IMMORTAL_LINE)) {
			return true;
		}
		State state = state(player);
		if (player.tickCount < state.immortalReadyAt) {
			return true;
		}
		state.immortalReadyAt = player.tickCount + IMMORTAL_LINE_COOLDOWN;
		player.setHealth(2.0F);
		player.clearFire();
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
		player.sendSystemMessage(
			Component.translatable("perk.pbenchants.immortal_line.held").withStyle(ChatFormatting.GOLD));
		return false;
	}

	// ---------- Living Armor ----------

	/**
	 * Experience mends the whole set at once, from any source and with no
	 * Mending on the pieces. True when the experience was spent on armour and
	 * should not also reach the bar — the same bargain Mending strikes, made
	 * across four pieces instead of one random item.
	 */
	public static boolean livingArmorAbsorbs(ServerPlayer player, int points) {
		if (points <= 0 || !ArmorPerks.owns(player, ArmorPerks.LIVING_ARMOR)) {
			return false;
		}
		boolean mended = false;
		for (EquipmentSlot slot : ArmorPerks.ARMOR_SLOTS) {
			ItemStack piece = player.getItemBySlot(slot);
			if (piece.isDamageableItem() && piece.getDamageValue() > 0) {
				piece.setDamageValue(Math.max(0, piece.getDamageValue() - points * LIVING_ARMOR_PER_POINT));
				mended = true;
			}
		}
		return mended;
	}
}
