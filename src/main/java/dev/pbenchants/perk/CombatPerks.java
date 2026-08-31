package dev.pbenchants.perk;

import dev.pbenchants.enchant.ModEnchantments;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.Holder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Everything the Sword tree needs to answer mid-swing, in one place: who owns
 * what, whether a node may fire at this target at all, and the damage the tree
 * adds on top of the weapon.
 *
 * <h2>The PvE-only rule</h2>
 *
 * <p>Nodes marked {@link dev.pbenchants.skill.SkillNode#pve()} do not apply
 * when the target is a player. Not scaled, not halved — {@link #appliesTo} says
 * no and the bonus never happens. Armour penetration, execute damage and
 * stacking damage-per-kill are all reasonable against a zombie and all rewrite
 * PvP into something nobody asked this mod to design, so the tree can be
 * generous in the fight it was built for. A server that wants them anyway flips
 * {@code pvp_perks} in the config.
 *
 * <p><b>Nostalgy is the exception</b> and is deliberately not routed through
 * this gate: a 1.8 attack cooldown is the one node here meant to be felt in a
 * duel. {@code nostalgy_pvp} turns even that off for servers that disagree.
 *
 * <h2>Combat state</h2>
 *
 * <p>Adrenaline, Bloodthirst, Hunter's Mark and Storm Bearer all need to
 * remember something between swings. It lives here, keyed by player id and
 * dropped on disconnect — none of it is worth saving, and a ramp that survived
 * a relog would be a bug rather than a reward.
 */
public final class CombatPerks {
	// --- node ids: keep in sync with the sword tree in SkillTrees ---
	public static final String COMBAT_MAGNET = "combat_magnet";
	public static final String BUTCHERS_CUT = "butchers_cut";
	public static final String BROAD_SWING = "broad_swing";
	public static final String SECOND_WIND = "second_wind";
	public static final String HUNTERS_MARK = "hunters_mark";
	public static final String RIPOSTE = "riposte";
	public static final String CLEAVE = "cleave";
	public static final String ADRENALINE = "adrenaline";
	public static final String STORM_BEARER = "storm_bearer";
	public static final String SHIELD_BREAKER = "shield_breaker";
	public static final String BLOODTHIRST = "bloodthirst";
	public static final String HEADHUNTER = "headhunter";
	public static final String SPOILS_OF_WAR = "spoils_of_war";
	public static final String WARLORDS_WAKE = "warlords_wake";
	public static final String DEATH_EYES = "death_eyes";

	/** Keen Edge damage per rank, at a full attack cooldown. */
	private static final float KEEN_EDGE_PER_RANK = 1.0F;

	/** Executioner: the health fraction below which a target counts as finishable. */
	private static final float EXECUTE_THRESHOLD = 0.3F;

	/** Executioner damage share per rank. */
	private static final float EXECUTE_PER_RANK = 0.15F;

	/** Ticks of unbroken combat before Adrenaline is at full strength. */
	private static final int ADRENALINE_RAMP_TICKS = 160;

	/** A gap longer than this ends the fight, and the ramp starts over. */
	private static final int ADRENALINE_LINGER_TICKS = 60;

	/** Adrenaline damage share at full ramp. */
	private static final float ADRENALINE_MAX = 0.15F;

	/** Bloodthirst: how long a kill keeps the stack alive, and how high it goes. */
	private static final int BLOODTHIRST_WINDOW_TICKS = 100;
	private static final int BLOODTHIRST_MAX_STACKS = 5;
	private static final float BLOODTHIRST_PER_STACK = 0.10F;

	/** Vanilla Smite's damage per level — what Death Eyes hands to everything else. */
	private static final float SMITE_PER_LEVEL = 2.5F;

	/** How long a Hunter's Mark outline lasts. */
	public static final int MARK_TICKS = 100;

	/** Cleave: how far the neighbour may stand, and how much of the hit it takes. */
	private static final double CLEAVE_RADIUS = 2.0;
	private static final float CLEAVE_SHARE = 0.33F;

	/** Warlord's Wake: the shockwave's reach and its share of the killing blow. */
	private static final double WAKE_RADIUS = 4.0;
	private static final float WAKE_SHARE = 0.5F;

	/** Phalanx: reach per rank, and the price of walking into a braced spear. */
	private static final double PHALANX_REACH_PER_RANK = 1.0;
	private static final double PHALANX_TOUCH_RANGE = 0.6;
	private static final float PHALANX_TOUCH_DAMAGE = 2.0F;
	private static final int PHALANX_INTERVAL_TICKS = 20;
	private static final Identifier PHALANX_REACH_ID =
		Identifier.fromNamespaceAndPath("pbenchants", "phalanx_reach");

	/** Per-player scratch state. Nothing here is saved. */
	public static final class State {
		public int combatStart;
		public int lastHit;
		public int bloodStacks;
		public int lastKill;
		/** The melee damage the player last put into a target, for Warlord's Wake. */
		public float lastMeleeDamage;
		/** True while {@code doSweepAttack} is running, so per-hit perks fire once. */
		public boolean inSweep;
		/** Game day Storm Bearer was last spent on, -1 for never. */
		public long stormBearerDay = Long.MIN_VALUE;
		/** The entity Hunter's Mark is lighting up, and when the light goes out. */
		public int markedEntityId = -1;
		public int markExpires;
		/** The entity the player's last critical hit landed on, for the tier-1 gate. */
		public int critTarget = -1;
		/** Tick a braced spear last pricked something, so Phalanx is not a blender. */
		public int lastPhalanxTick;
	}

	/**
	 * Concurrent because this is a common class: in single-player the render
	 * thread reads it through the client's copy of the attack code while the
	 * integrated server thread writes it.
	 */
	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	private CombatPerks() {
	}

	public static State state(Player player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
	}

	/** Drops a leaver's ramp, stacks and mark. */
	public static void forget(Player player) {
		STATES.remove(player.getUUID());
	}

	// ---------- ownership ----------

	/** Does this player own a sword-tree node? Answers the same on both sides. */
	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.SWORD, nodeId);
	}

	/** Highest owned rank of a ranked node family ("keen_edge" → keen_edge_1..3). */
	public static int rank(Player player, String baseId, int maxRank) {
		int level = 0;
		for (int r = 1; r <= maxRank; r++) {
			if (owns(player, baseId + "_" + r)) {
				level = r;
			}
		}
		return level;
	}

	/**
	 * The level of one of our combat enchantments the item is actually worth in
	 * this player's hands — the item's rank clamped to theirs, 0 if anything on
	 * it is unearned. Every effect below reads this rather than the raw stack.
	 */
	public static int level(Player player, ItemStack stack, ResourceKey<Enchantment> key) {
		return ItemAuthority.effectiveLevel(player, stack, key);
	}

	public static int mainHandLevel(Player player, ResourceKey<Enchantment> key) {
		return level(player, player.getMainHandItem(), key);
	}

	// ---------- the PvE-only rule ----------

	/**
	 * May a PvE-only node fire at this target? One check, one place — every
	 * effect asks this rather than carrying its own {@code instanceof Player}.
	 */
	public static boolean appliesTo(Entity target) {
		return !(target instanceof Player) || PBEnchantsConfig.pvpPerks();
	}

	/** Nostalgy's own switch: on by default, because the node is a PvP node. */
	public static boolean nostalgyAppliesInPvp() {
		return PBEnchantsConfig.nostalgyPvp();
	}

	/**
	 * The share of a hit to give back when Nostalgy is switched off for PvP.
	 *
	 * <p>An attack cooldown cannot be made target-specific: it is one timer on
	 * the attacker, not a property of who is standing in front of them. So a
	 * server that turns {@code nostalgy_pvp} off does not get a slower swing
	 * against players, it gets the same swing paying <em>vanilla</em> damage.
	 * Vanilla scales a hit by {@code 0.2 + scale x scale x 0.8}, so handing back
	 * the ratio of the two curves lands on the number the player would have done
	 * without the node.
	 */
	public static float pvpCooldownRatio(float vanillaScale, float actualScale) {
		float vanilla = 0.2F + vanillaScale * vanillaScale * 0.8F;
		float actual = 0.2F + actualScale * actualScale * 0.8F;
		return actual <= 0.0F ? 1.0F : Math.min(1.0F, vanilla / actual);
	}

	// ---------- damage ----------

	/**
	 * What the tree adds to one melee hit, on top of the weapon and vanilla's
	 * own enchantments. Called from {@code Player.getEnchantedDamage}, which is
	 * the one point where the attacker, the target and the base damage are all
	 * in hand — and which a locked item never reaches, so a borrowed sword adds
	 * nothing here either.
	 *
	 * @param cooldownScale how full the <em>vanilla</em> attack cooldown was,
	 *                      0..1. Deliberately the vanilla number: Nostalgy makes
	 *                      the real one always 1, and Keen Edge paying out on
	 *                      that would turn a timing reward into a flat bonus.
	 */
	public static float damageBonus(Player attacker, Entity target, float damage, float cooldownScale) {
		ItemStack weapon = attacker.getMainHandItem();
		float bonus = 0.0F;

		int keenEdge = level(attacker, weapon, ModEnchantments.KEEN_EDGE);
		if (keenEdge > 0) {
			bonus += keenEdge * KEEN_EDGE_PER_RANK * cooldownScale;
		}

		if (!(target instanceof LivingEntity living)) {
			return bonus;
		}
		boolean pve = appliesTo(target);

		int executioner = level(attacker, weapon, ModEnchantments.EXECUTIONER);
		if (pve && executioner > 0 && living.getMaxHealth() > 0.0F
			&& living.getHealth() / living.getMaxHealth() < EXECUTE_THRESHOLD) {
			bonus += damage * executioner * EXECUTE_PER_RANK;
		}

		if (pve && owns(attacker, ADRENALINE)) {
			bonus += damage * adrenalineShare(attacker);
		}

		if (pve && owns(attacker, BLOODTHIRST)) {
			bonus += damage * bloodthirstShare(attacker);
		}

		if (pve && owns(attacker, DEATH_EYES)) {
			bonus += deathEyesBonus(attacker, living);
		}
		return bonus;
	}

	/** Adrenaline's current share: ramps over eight seconds of unbroken combat. */
	public static float adrenalineShare(Player player) {
		State state = state(player);
		int now = player.tickCount;
		if (state.lastHit == 0 || now - state.lastHit > ADRENALINE_LINGER_TICKS) {
			return 0.0F;
		}
		float ramp = Math.min(1.0F, (now - state.combatStart) / (float) ADRENALINE_RAMP_TICKS);
		return ADRENALINE_MAX * Math.max(0.0F, ramp);
	}

	/** Bloodthirst's current share: +10% per stack, five stacks, five seconds each. */
	public static float bloodthirstShare(Player player) {
		State state = state(player);
		if (state.bloodStacks <= 0 || player.tickCount - state.lastKill > BLOODTHIRST_WINDOW_TICKS) {
			return 0.0F;
		}
		return state.bloodStacks * BLOODTHIRST_PER_STACK;
	}

	/**
	 * Death Eyes: you see every mob as one of the dead, and so does your sword.
	 *
	 * <p>Vanilla drives Smite off the {@code #minecraft:sensitive_to_smite}
	 * entity-type tag, and widening that tag in a data pack would hand the bonus
	 * to every player on the server. So the bonus is recomputed here, for this
	 * holder, on this hit — the same shape as the per-player enchantment
	 * ceilings the mod already does. Targets vanilla already counts as undead
	 * are skipped: they got Smite the normal way and must not get it twice.
	 *
	 * <p>It changes what Smite considers undead and <em>nothing else</em>. Not
	 * drops, not Bane of Arthropods, not zombie behaviour, not what another
	 * player's weapon sees.
	 */
	private static float deathEyesBonus(Player attacker, LivingEntity target) {
		if (target.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
			return 0.0F;
		}
		Holder<Enchantment> smite = attacker.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(Enchantments.SMITE)
			.orElse(null);
		if (smite == null) {
			return 0.0F;
		}
		int level = EnchantmentHelper.getItemEnchantmentLevel(smite, attacker.getMainHandItem());
		return level * SMITE_PER_LEVEL;
	}

	// ---------- events the effects hang off ----------

	/** A melee hit landed: feeds Adrenaline's ramp and remembers the blow. */
	public static void onMeleeHit(Player player, float damage) {
		State state = state(player);
		int now = player.tickCount;
		if (state.lastHit == 0 || now - state.lastHit > ADRENALINE_LINGER_TICKS) {
			state.combatStart = now;
		}
		state.lastHit = now;
		state.lastMeleeDamage = damage;
	}

	/**
	 * Cleave: an axe hit passes a third of itself to one mob standing next to
	 * the target. One neighbour, not all of them — a cone of splash damage is a
	 * different node, and this one is meant to make an axe feel wide rather than
	 * to hand it the sweep the sword tree already sells.
	 */
	public static void cleave(ServerPlayer attacker, LivingEntity target, float damage) {
		if (damage <= 0.0F || !owns(attacker, CLEAVE)
			|| !attacker.getMainHandItem().is(ItemTags.AXES)
			|| !(attacker.level() instanceof ServerLevel level)) {
			return;
		}
		LivingEntity neighbour = nearest(level, attacker, target, CLEAVE_RADIUS);
		if (neighbour != null) {
			neighbour.hurtServer(level, attacker.damageSources().playerAttack(attacker), damage * CLEAVE_SHARE);
		}
	}

	/**
	 * Warlord's Wake: a killing blow goes off like a shockwave, dealing half of
	 * itself to everything within four blocks. The blow that did it is the one
	 * this player last put into something, read from the state rather than from
	 * the death — a death source knows what kind of damage killed, not how big
	 * the swing was.
	 */
	public static void warlordsWake(ServerPlayer attacker, LivingEntity victim) {
		if (!owns(attacker, WARLORDS_WAKE) || !(attacker.level() instanceof ServerLevel level)) {
			return;
		}
		float damage = state(attacker).lastMeleeDamage * WAKE_SHARE;
		if (damage <= 0.0F) {
			return;
		}
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
			victim.getBoundingBox().inflate(WAKE_RADIUS))) {
			if (nearby == attacker || nearby == victim || nearby.isDeadOrDying()
				|| !appliesTo(nearby) || attacker.isAlliedTo(nearby)) {
				continue;
			}
			nearby.hurtServer(level, attacker.damageSources().playerAttack(attacker), damage);
		}
	}

	/**
	 * Phalanx: a braced spear is a wall. While the player holds one and is
	 * winding it up, anything standing against them takes the point — once a
	 * second, so holding a corridor is a stand rather than a blender. The reach
	 * half of the node is an attribute modifier kept in step by the same tick.
	 */
	public static void phalanxTick(ServerPlayer player) {
		int phalanx = mainHandLevel(player, ModEnchantments.PHALANX);
		updateReach(player, phalanx);
		if (phalanx < 2 || !player.isUsingItem() || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		State state = state(player);
		if (player.tickCount - state.lastPhalanxTick < PHALANX_INTERVAL_TICKS) {
			return;
		}
		float damage = PHALANX_TOUCH_DAMAGE * phalanx;
		boolean pricked = false;
		for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
			player.getBoundingBox().inflate(PHALANX_TOUCH_RANGE))) {
			if (nearby == player || nearby.isDeadOrDying() || !appliesTo(nearby)
				|| player.isAlliedTo(nearby)) {
				continue;
			}
			nearby.hurtServer(level, player.damageSources().playerAttack(player), damage);
			pricked = true;
		}
		if (pricked) {
			state.lastPhalanxTick = player.tickCount;
		}
	}

	/**
	 * Keeps the Phalanx reach modifier matching the spear in hand. Transient, so
	 * it never reaches the save file: a modifier that outlived the item would be
	 * a permanent reach hack rather than a node.
	 */
	private static void updateReach(ServerPlayer player, int phalanxLevel) {
		AttributeInstance reach = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
		if (reach == null) {
			return;
		}
		AttributeModifier current = reach.getModifier(PHALANX_REACH_ID);
		double wanted = phalanxLevel * PHALANX_REACH_PER_RANK;
		if (wanted <= 0.0) {
			if (current != null) {
				reach.removeModifier(PHALANX_REACH_ID);
			}
			return;
		}
		if (current != null && current.amount() == wanted) {
			return;
		}
		reach.removeModifier(PHALANX_REACH_ID);
		reach.addTransientModifier(new AttributeModifier(PHALANX_REACH_ID, wanted,
			AttributeModifier.Operation.ADD_VALUE));
	}

	/** The closest other living thing to {@code target}, or null. */
	private static LivingEntity nearest(ServerLevel level, ServerPlayer attacker, LivingEntity target,
			double radius) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
			target.getBoundingBox().inflate(radius))) {
			if (candidate == target || candidate == attacker || candidate.isDeadOrDying()
				|| !appliesTo(candidate) || attacker.isAlliedTo(candidate)) {
				continue;
			}
			double distance = candidate.distanceToSqr(target);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		return best;
	}

	/** A kill landed: Bloodthirst stacks, Second Wind pays out. */
	public static void onKill(ServerPlayer player, LivingEntity victim) {
		State state = state(player);
		int now = player.tickCount;
		if (owns(player, BLOODTHIRST) && appliesTo(victim)) {
			boolean chained = state.bloodStacks > 0 && now - state.lastKill <= BLOODTHIRST_WINDOW_TICKS;
			state.bloodStacks = chained ? Math.min(BLOODTHIRST_MAX_STACKS, state.bloodStacks + 1) : 1;
			state.lastKill = now;
		}
		if (owns(player, SECOND_WIND)) {
			float saturation = player.getFoodData().getSaturationLevel();
			player.getFoodData().setSaturation(Math.min(player.getFoodData().getFoodLevel(), saturation + 1.0F));
		}
	}

	/**
	 * Hunter's Mark: the mob you last hit is outlined, and its remaining health
	 * goes on the action bar. The outline is vanilla's glowing tag, cleared
	 * again by {@link #tick} — an entity left glowing forever would be a bug
	 * every player could see.
	 */
	public static void mark(ServerPlayer player, LivingEntity target) {
		if (!owns(player, HUNTERS_MARK)) {
			return;
		}
		State state = state(player);
		clearMark(player, state);
		target.setGlowingTag(true);
		state.markedEntityId = target.getId();
		state.markExpires = player.tickCount + MARK_TICKS;
		player.sendSystemMessage(Component.translatable("perk.pbenchants.hunters_mark.readout",
			target.getDisplayName(), String.format("%.1f", target.getHealth()),
			String.format("%.1f", target.getMaxHealth())), true);
	}

	/** Every tick: keeps the spear honest and lets the mark burn out. */
	public static void tick(ServerPlayer player) {
		phalanxTick(player);
		State state = STATES.get(player.getUUID());
		if (state != null && state.markedEntityId != -1 && player.tickCount >= state.markExpires) {
			clearMark(player, state);
		}
	}

	/** A critical hit landed, remembered so the tier-1 gate can ask about the kill. */
	public static void onCrit(Player player, Entity target) {
		state(player).critTarget = target.getId();
	}

	private static void clearMark(ServerPlayer player, State state) {
		if (state.markedEntityId == -1) {
			return;
		}
		Entity previous = player.level().getEntity(state.markedEntityId);
		if (previous != null) {
			previous.setGlowingTag(false);
		}
		state.markedEntityId = -1;
	}
}
