package dev.toolmastery.perk;

import dev.toolmastery.enchant.ModEnchantments;
import dev.toolmastery.mixin.ProjectileWeaponItemInvoker;
import dev.toolmastery.skill.SkillTrees;
import dev.toolmastery.track.BowTracker;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Everything the Bow tree needs to answer between the draw and the impact, in
 * one place: how fast the draw is, how fast <em>you</em> are while drawing,
 * and what the arrow is worth when it finally lands.
 *
 * <h2>The PvE-only rule, at range</h2>
 *
 * <p>This tree carries more {@code .pve()} nodes than any other — eleven —
 * because distance is a sharper knife against players than against mobs:
 * distance-scaled damage, a root, a kill that bounces, an outline through
 * walls. All of them route through {@link CombatPerks#appliesTo}, the same
 * single gate the Sword tree uses, so {@code pvp_perks} in the config flips
 * them all at once. What deliberately stays live in a duel: Swift Draw and
 * Rapid Reload (moving while you aim is visible and symmetric — the class's
 * Nostalgy), Fletcher's Hands, Steady Aim, Gale, and the two non-damage
 * capstones.
 *
 * <p><b>Multishot Focus</b> is the one PvE gate that cannot be applied at fire
 * time — a volley in flight has no target yet. So the convergence itself always
 * happens, and the two side arrows are tagged; when a tagged arrow hits a
 * player on a PvE-rules server it lands at a third, so a converged volley on a
 * player is worth roughly one vanilla shot.
 *
 * <h2>Damage attribution</h2>
 *
 * <p>Arrow damage is applied by the projectile, not the player, so everything
 * here reads the arrow at impact: the owner, the weapon it was fired from
 * (which travels on the arrow entity), and the point it was launched at. An
 * arrow in flight when the shooter's tree changes is therefore judged by the
 * tree as it stands at impact — same rule as a sword changing mid-swing, and
 * one line of code instead of a snapshot.
 */
public final class BowPerks {
	// --- node ids: keep in sync with the bow tree in SkillTrees ---
	public static final String FLETCHERS_HANDS = "fletchers_hands";
	public static final String SWIFT_DRAW = "swift_draw";
	public static final String QUIVER_SENSE = "quiver_sense";
	public static final String ARROW_RECOVERY = "arrow_recovery";
	public static final String STEADY_AIM = "steady_aim";
	public static final String FLETCHERS_BENCH = "fletchers_bench";
	public static final String PIERCING_SIGHT = "piercing_sight";
	public static final String RAPID_RELOAD = "rapid_reload";
	public static final String MULTISHOT_FOCUS = "multishot_focus";
	public static final String ALCHEMISTS_QUIVER = "alchemists_quiver";
	public static final String AERIAL_HUNTER = "aerial_hunter";
	public static final String ENDLESS_QUIVER = "endless_quiver";
	public static final String DEADEYE = "deadeye";
	public static final String STORM_OF_ARROWS = "storm_of_arrows";
	public static final String HUNTERS_BOUNTY = "hunters_bounty";

	/** Fletcher's Hands: draw and load speed gained per rank. */
	private static final float DRAW_SPEED_PER_RANK = 0.2F;

	/** Swift Draw: movement while aiming, per rank, on top of vanilla's 20%. */
	private static final float SWIFT_DRAW_BASE = 0.2F;
	private static final float SWIFT_DRAW_PER_RANK = 0.2F;

	/** Long Shot: the range the bonus starts at, and its share per rank. */
	private static final double LONG_SHOT_RANGE = 25.0;
	private static final float LONG_SHOT_PER_RANK = 0.10F;

	/** Deadeye: a fully drawn shot landing past this is a guaranteed critical. */
	private static final double DEADEYE_RANGE = 50.0;
	private static final float DEADEYE_FACTOR = 2.0F;

	/** Aerial Hunter's share against anything not standing on the ground. */
	private static final float AERIAL_FACTOR = 1.5F;

	/** Gale: arrow gravity cut per rank. */
	private static final double GALE_CUT_PER_RANK = 0.3;

	/** Ricochet: the bounce's reach and share, per rank. */
	private static final double[] RICOCHET_RADIUS = {0.0, 8.0, 12.0};
	private static final float[] RICOCHET_SHARE = {0.0F, 0.5F, 0.75F};

	/** Pinning Shot: 1.5 seconds of not going anywhere. */
	private static final int PINNING_TICKS = 30;
	private static final int PINNING_AMPLIFIER = 6;

	/** Arrow Recovery: return chance per rank. */
	private static final float RECOVERY_PER_RANK = 0.25F;

	/** Multishot Focus: what a tagged side arrow is worth against a player. */
	private static final float FOCUS_PVP_SHARE = 0.34F;

	/** How long a Piercing Sight outline lasts. */
	public static final int SIGHT_TICKS = 60;

	/** Alchemist's Quiver: tipped-arrow effects last half again as long. */
	private static final float QUIVER_DURATION_SCALE = 1.5F;

	/** Storm of Arrows: ticks of over-draw that bank one more arrow, and the cap. */
	private static final int STORM_BANK_TICKS = 20;
	private static final int STORM_MAX_EXTRA = 2;
	private static final float STORM_SPREAD = 2.0F;

	/** Rapid Reload II: ticks between two background loads. */
	private static final int BACKGROUND_LOAD_TICKS = 100;

	/** Per-player scratch state. Nothing here is saved. */
	public static final class State {
		/** The entity Piercing Sight is lighting up, and when the light goes out. */
		public int markedEntityId = -1;
		public int markExpires;
		/** The arrow damage this player last put into something, for Ricochet. */
		public float lastArrowDamage;
		/** Tick a crossbow was last loaded in the background, so it is a rhythm. */
		public int lastBackgroundLoad;
		/** Arrows the current over-draw has banked, for the action-bar readout. */
		public int stormBanked;
	}

	private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

	/**
	 * The two side arrows of a converged Multishot volley, by entity id —
	 * see the class comment for why the PvE gate has to travel with them.
	 * Ids are dropped again when the arrow lands or despawns.
	 */
	private static final Set<Integer> FOCUSED_ARROWS = ConcurrentHashMap.newKeySet();

	private BowPerks() {
	}

	public static State state(Player player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
	}

	/** Drops a leaver's mark and rhythm. */
	public static void forget(Player player) {
		STATES.remove(player.getUUID());
	}

	// ---------- ownership ----------

	/** Does this player own a bow-tree node? Answers the same on both sides. */
	public static boolean owns(Player player, String nodeId) {
		return PerkAccess.owns(player, SkillTrees.BOW, nodeId);
	}

	/** Highest owned rank of a ranked node family ("swift_draw" → swift_draw_1..3). */
	public static int rank(Player player, String baseId, int maxRank) {
		int level = 0;
		for (int r = 1; r <= maxRank; r++) {
			if (owns(player, baseId + "_" + r)) {
				level = r;
			}
		}
		return level;
	}

	/** One of our ranged enchantments, clamped to what this holder has earned. */
	public static int level(Player player, ItemStack stack, ResourceKey<Enchantment> key) {
		return ItemAuthority.effectiveLevel(player, stack, key);
	}

	// ---------- the draw ----------

	/**
	 * Fletcher's Hands, bow half: the draw counts 20/40/60% more ticks than
	 * really passed, so full power arrives sooner. Applied to the tick count
	 * rather than the power curve so the curve itself stays vanilla.
	 */
	public static int scaledDrawTicks(LivingEntity shooter, int ticks) {
		if (!(shooter instanceof Player player)) {
			return ticks;
		}
		int rank = rank(player, FLETCHERS_HANDS, 3);
		return rank <= 0 ? ticks : (int) (ticks * (1.0F + DRAW_SPEED_PER_RANK * rank));
	}

	/** Fletcher's Hands, crossbow half: the charge takes 17/29/38% less time. */
	public static int scaledChargeDuration(LivingEntity shooter, int vanilla) {
		if (!(shooter instanceof Player player)) {
			return vanilla;
		}
		int rank = rank(player, FLETCHERS_HANDS, 3);
		return rank <= 0 ? vanilla : Math.max(1, (int) (vanilla / (1.0F + DRAW_SPEED_PER_RANK * rank)));
	}

	/**
	 * Swift Draw and Rapid Reload I, the movement half of the class. Vanilla
	 * moves an aiming player at the item's {@code use_effects} multiplier — 20%
	 * for a bow — and this lifts it to 40/60/80% per Swift Draw rank. Rapid
	 * Reload I goes further for the crossbow alone: no slowdown at all, which
	 * together with {@link #sprintWhileUsing} is what "keeps loading while you
	 * sprint" means. Client-side by nature: player movement is
	 * client-authoritative, so the multiplier the input pipeline reads is the
	 * whole rule.
	 */
	public static float useSpeedMultiplier(Player player, ItemStack useItem, float vanilla) {
		if (useItem.getItem() instanceof CrossbowItem && owns(player, RAPID_RELOAD + "_1")) {
			return 1.0F;
		}
		if (!(useItem.getItem() instanceof BowItem) && !(useItem.getItem() instanceof CrossbowItem)) {
			return vanilla;
		}
		int rank = rank(player, SWIFT_DRAW, 3);
		return rank <= 0 ? vanilla : Math.max(vanilla, SWIFT_DRAW_BASE + SWIFT_DRAW_PER_RANK * rank);
	}

	/** Rapid Reload I: a charging crossbow does not end (or forbid) a sprint. */
	public static boolean sprintWhileUsing(Player player, ItemStack useItem) {
		return useItem.getItem() instanceof CrossbowItem && owns(player, RAPID_RELOAD + "_1");
	}

	/** Steady Aim: an arrow loosed while sneaking flies with no spread at all. */
	public static float steadyInaccuracy(LivingEntity shooter, float inaccuracy) {
		if (shooter instanceof Player player && player.isShiftKeyDown() && owns(player, STEADY_AIM)) {
			return 0.0F;
		}
		return inaccuracy;
	}

	// ---------- the impact ----------

	/**
	 * The one point where an arrow, its shooter, its weapon and its target are
	 * all in hand — the ranged mirror of {@code CombatPerks.damageBonus}. The
	 * caller redirects vanilla's own enchantment pass through here, which buys
	 * the tree two things at once: a <b>locked</b> weapon (carrying a Tool
	 * Mastery enchantment the holder never earned) skips that pass entirely and
	 * lands at bare-bow damage, and every bonus below reads levels already
	 * clamped to what the shooter has earned.
	 *
	 * @param origin where the arrow was loosed, or null for an arrow that was
	 *               already flying when the server started
	 */
	public static float arrowDamage(ServerLevel level, AbstractArrow arrow, @Nullable Vec3 origin,
			ItemStack weapon, Entity target, DamageSource source, float base) {
		if (!(arrow.getOwner() instanceof ServerPlayer shooter)) {
			return EnchantmentHelper.modifyDamage(level, weapon, target, source, base);
		}
		float damage = ItemAuthority.locked(shooter, weapon)
			? base
			: EnchantmentHelper.modifyDamage(level, weapon, target, source, base);

		BowTracker.onArrowHit(shooter);
		boolean pve = CombatPerks.appliesTo(target);
		double distance = (origin != null ? origin : shooter.position()).distanceTo(arrow.position());

		int longShot = level(shooter, weapon, ModEnchantments.LONG_SHOT);
		if (pve && longShot > 0 && distance > LONG_SHOT_RANGE) {
			damage *= 1.0F + LONG_SHOT_PER_RANK * longShot;
		}
		if (pve && owns(shooter, AERIAL_HUNTER)
			&& (!target.onGround() || shooter.isFallFlying())) {
			damage *= AERIAL_FACTOR;
		}
		if (pve && owns(shooter, DEADEYE) && arrow.isCritArrow() && distance > DEADEYE_RANGE) {
			damage *= DEADEYE_FACTOR;
		}
		// A tagged Multishot Focus side arrow that found a player on a
		// PvE-rules server lands at a third — the volley is worth one shot.
		if (target instanceof Player && !ToolMasteryConfig.pvpPerks()
			&& FOCUSED_ARROWS.contains(arrow.getId())) {
			damage *= FOCUS_PVP_SHARE;
		}

		if (target instanceof LivingEntity living && pve) {
			int pinning = weapon.is(Items.CROSSBOW) ? level(shooter, weapon, ModEnchantments.PINNING_SHOT) : 0;
			if (pinning > 0) {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
					PINNING_TICKS, PINNING_AMPLIFIER, false, true), shooter);
			}
			sight(shooter, living);
		}

		state(shooter).lastArrowDamage = damage;
		return damage;
	}

	/**
	 * Gale: the weapon's gravity cut, read off the arrow. Server-authoritative —
	 * the client's copy of the arrow does not know the weapon it came from, so
	 * observers see the position packets pull the arc flat rather than
	 * predicting it. The trade was deliberate: syncing the weapon to every
	 * watcher for a smoother curve is a protocol change, and the shooter (the
	 * one actually aiming) leads their own shots by the landing point the
	 * server reports.
	 */
	public static double galeGravityFactor(AbstractArrow arrow) {
		ItemStack weapon = arrow.getWeaponItem();
		if (weapon == null || weapon.isEmpty() || !(arrow.getOwner() instanceof Player shooter)) {
			return 1.0;
		}
		int gale = level(shooter, weapon, ModEnchantments.GALE);
		return gale <= 0 ? 1.0 : Math.max(0.0, 1.0 - GALE_CUT_PER_RANK * gale);
	}

	/**
	 * Ricochet: an arrow that kills bounces to a second target nearby, at half
	 * (rank I) or three quarters (rank II) of the blow. The bounce is applied
	 * as direct arrow damage rather than a second entity — a real arrow could
	 * miss, and a node that sometimes does nothing reads as broken.
	 */
	public static void ricochet(ServerPlayer shooter, LivingEntity victim, AbstractArrow arrow) {
		ItemStack weapon = arrow.getWeaponItem();
		if (weapon == null || weapon.isEmpty() || !(shooter.level() instanceof ServerLevel level)) {
			return;
		}
		int rank = level(shooter, weapon, ModEnchantments.RICOCHET);
		float damage = state(shooter).lastArrowDamage * RICOCHET_SHARE[Math.min(rank, 2)];
		if (rank <= 0 || damage <= 0.0F) {
			return;
		}
		LivingEntity next = nearestTarget(level, shooter, victim, RICOCHET_RADIUS[Math.min(rank, 2)]);
		if (next != null) {
			next.hurtServer(level, shooter.damageSources().arrow(arrow, shooter), damage);
			level.playSound(null, next.getX(), next.getY(), next.getZ(),
				SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.8F, 1.4F);
		}
	}

	/** The closest living thing to {@code from} a PvE node may fire at, or null. */
	@Nullable
	private static LivingEntity nearestTarget(ServerLevel level, ServerPlayer shooter, LivingEntity from,
			double radius) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
			from.getBoundingBox().inflate(radius))) {
			if (candidate == from || candidate == shooter || candidate.isDeadOrDying()
				|| !CombatPerks.appliesTo(candidate) || shooter.isAlliedTo(candidate)) {
				continue;
			}
			double distance = candidate.distanceToSqr(from);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = candidate;
			}
		}
		return best;
	}

	/**
	 * Piercing Sight: the mob you hit is outlined for three seconds, with its
	 * health on the action bar — the archer's Hunter's Mark, at the range where
	 * losing a target in the dark actually happens.
	 */
	private static void sight(ServerPlayer shooter, LivingEntity target) {
		if (!owns(shooter, PIERCING_SIGHT) || !CombatPerks.appliesTo(target)) {
			return;
		}
		State state = state(shooter);
		clearSight(shooter, state);
		target.setGlowingTag(true);
		state.markedEntityId = target.getId();
		state.markExpires = shooter.tickCount + SIGHT_TICKS;
		shooter.sendSystemMessage(Component.translatable("perk.toolmastery.piercing_sight.readout",
			target.getDisplayName(), String.format("%.1f", target.getHealth()),
			String.format("%.1f", target.getMaxHealth())), true);
	}

	private static void clearSight(ServerPlayer player, State state) {
		if (state.markedEntityId == -1) {
			return;
		}
		Entity previous = player.level().getEntity(state.markedEntityId);
		if (previous != null) {
			previous.setGlowingTag(false);
		}
		state.markedEntityId = -1;
	}

	/** Alchemist's Quiver: tipped-arrow effects last half again as long, on mobs. */
	public static float potionDurationScale(Arrow arrow, LivingEntity target, float vanilla) {
		if (arrow.getOwner() instanceof Player shooter && owns(shooter, ALCHEMISTS_QUIVER)
			&& CombatPerks.appliesTo(target)) {
			return vanilla * QUIVER_DURATION_SCALE;
		}
		return vanilla;
	}

	// ---------- the quiver ----------

	/**
	 * Multishot Focus: the volley's side arrows fire at angle zero instead of
	 * ±10°, so all three can land on one target. The side arrows are tagged on
	 * the way out for the impact-side PvP dampener.
	 */
	public static float focusedAngle(LivingEntity shooter, Projectile projectile, int index, float angle) {
		if (!(shooter instanceof Player player) || !owns(player, MULTISHOT_FOCUS)) {
			return angle;
		}
		if (index != 0) {
			FOCUSED_ARROWS.add(projectile.getId());
		}
		return 0.0F;
	}

	/** An arrow landed or despawned: Arrow Recovery's roll, and the focus tag drops. */
	public static void onArrowGone(AbstractArrow arrow) {
		FOCUSED_ARROWS.remove(arrow.getId());
		if (arrow.pickup != AbstractArrow.Pickup.ALLOWED
			|| !(arrow.getOwner() instanceof ServerPlayer shooter) || shooter.isRemoved()) {
			return;
		}
		int rank = rank(shooter, ARROW_RECOVERY, 2);
		if (rank <= 0 || arrow.level().getRandom().nextFloat() >= RECOVERY_PER_RANK * rank) {
			return;
		}
		ItemStack returned = arrow.getPickupItemStackOrigin().copyWithCount(1);
		if (!shooter.getInventory().add(returned)) {
			shooter.drop(returned, false);
		}
	}

	/**
	 * Endless Quiver, half one: Infinity's "consume nothing" also answers for
	 * spectral arrows. Returning zero here rides vanilla's own path — the
	 * fired arrow gets the intangible tag and cannot be picked back up, so
	 * there is no dupe to close. (Half two — Infinity and Mending on one bow —
	 * is the data pack plus the anvil gate.)
	 */
	public static int ammoUse(LivingEntity shooter, ItemStack weapon, ItemStack ammo, int vanilla) {
		if (vanilla <= 0 || !ammo.is(Items.SPECTRAL_ARROW)
			|| !(shooter instanceof Player player) || !owns(player, ENDLESS_QUIVER)) {
			return vanilla;
		}
		return vanillaEnchantLevel(player, weapon, Enchantments.INFINITY) > 0 ? 0 : vanilla;
	}

	/**
	 * Rapid Reload II: a crossbow riding in the inventory loads itself. Every
	 * five seconds one uncharged crossbow that is <em>not</em> in the active
	 * hand draws ammunition exactly the way a held reload would — same
	 * Multishot copies, same Infinity rules — and clicks shut.
	 */
	public static void slowTick(ServerPlayer player) {
		if (!owns(player, RAPID_RELOAD + "_2")
			|| player.tickCount - state(player).lastBackgroundLoad < BACKGROUND_LOAD_TICKS) {
			return;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack crossbow = inventory.getItem(slot);
			if (!crossbow.is(Items.CROSSBOW) || CrossbowItem.isCharged(crossbow)
				|| crossbow == player.getMainHandItem() || crossbow == player.getOffhandItem()) {
				continue;
			}
			ItemStack ammo = player.getProjectile(crossbow);
			if (ammo.isEmpty()) {
				return;
			}
			List<ItemStack> loaded = ProjectileWeaponItemInvoker.toolmastery$draw(crossbow, ammo, player);
			if (loaded.isEmpty()) {
				return;
			}
			crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(loaded));
			state(player).lastBackgroundLoad = player.tickCount;
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 0.4F, 1.0F);
			return;
		}
	}

	// ---------- capstones ----------

	/**
	 * Storm of Arrows: holding the draw past full charge banks up to two more
	 * arrows, and the release is a volley. Each banked arrow is real — it costs
	 * ammunition, it can be tipped, it can be picked back up — so the capstone
	 * is patience turned into a broadside rather than free damage.
	 */
	public static void stormOfArrows(ServerLevel level, ServerPlayer shooter, ItemStack weapon,
			int scaledUseTicks) {
		if (!owns(shooter, STORM_OF_ARROWS)) {
			return;
		}
		int extra = Math.min(STORM_MAX_EXTRA, Math.max(0, (scaledUseTicks - STORM_BANK_TICKS) / STORM_BANK_TICKS));
		state(shooter).stormBanked = 0;
		for (int i = 0; i < extra; i++) {
			ItemStack ammo = shooter.getProjectile(weapon);
			if (ammo.isEmpty()) {
				return;
			}
			boolean free = shooter.hasInfiniteMaterials()
				|| (ammo.is(Items.ARROW) && vanillaEnchantLevel(shooter, weapon, Enchantments.INFINITY) > 0);
			AbstractArrow arrow = ammo.is(Items.SPECTRAL_ARROW)
				? new SpectralArrow(level, shooter, ammo.copyWithCount(1), weapon)
				: new Arrow(level, shooter, ammo.copyWithCount(1), weapon);
			arrow.setCritArrow(true);
			if (free) {
				arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			} else {
				ammo.shrink(1);
			}
			arrow.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), 0.0F, 3.0F, STORM_SPREAD);
			level.addFreshEntity(arrow);
			BowTracker.onArrowsFired(shooter, weapon, List.of(arrow.getPickupItemStackOrigin()));
		}
		if (extra > 0) {
			level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
				SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
	}

	// ---------- housekeeping ----------

	/**
	 * Every tick: the Piercing Sight outline goes out on time, and an over-drawn
	 * Storm of Arrows tells its archer what the volley is currently worth.
	 */
	public static void tick(ServerPlayer player) {
		State state = STATES.get(player.getUUID());
		if (state != null && state.markedEntityId != -1 && player.tickCount >= state.markExpires) {
			clearSight(player, state);
		}
		if (owns(player, STORM_OF_ARROWS) && player.isUsingItem()
			&& player.getUseItem().getItem() instanceof BowItem) {
			int scaled = scaledDrawTicks(player, player.getTicksUsingItem());
			int banked = Math.min(STORM_MAX_EXTRA, Math.max(0, (scaled - STORM_BANK_TICKS) / STORM_BANK_TICKS));
			State s = state(player);
			if (banked != s.stormBanked) {
				s.stormBanked = banked;
				if (banked > 0) {
					player.sendSystemMessage(Component.translatable(
						"perk.toolmastery.storm_of_arrows.banked", 1 + banked), true);
				}
			}
		}
	}

	/**
	 * Fletcher's Bench, half one: arrows leave the crafting grid in double
	 * yield. Applied to the taken amount, so shift-crafting a stack doubles the
	 * stack rather than one batch.
	 */
	public static void onCraftTake(ServerPlayer player, ItemStack stack, int amount) {
		if (amount <= 0 || !owns(player, FLETCHERS_BENCH)) {
			return;
		}
		if (stack.is(Items.ARROW) || stack.is(Items.TIPPED_ARROW) || stack.is(Items.SPECTRAL_ARROW)) {
			ItemStack bonus = stack.copyWithCount(amount);
			if (!player.getInventory().add(bonus)) {
				player.drop(bonus, false);
			}
		}
	}

	/** Fletcher's Bench, half two: chickens and parrots part with one more feather. */
	public static void featherBounty(ServerPlayer killer, LivingEntity victim) {
		if (!owns(killer, FLETCHERS_BENCH)
			|| (victim.getType() != EntityTypes.CHICKEN && victim.getType() != EntityTypes.PARROT)
			|| !(victim.level() instanceof ServerLevel level)) {
			return;
		}
		level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,
			victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.FEATHER)));
	}

	/** A vanilla enchantment's raw level on a stack, for the rules that read them. */
	private static int vanillaEnchantLevel(Player player, ItemStack stack, ResourceKey<Enchantment> key) {
		Holder<Enchantment> holder = player.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT)
			.get(key)
			.orElse(null);
		return holder == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
	}
}
