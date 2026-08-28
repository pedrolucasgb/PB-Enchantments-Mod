package dev.toolmastery.perk;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What a kill leaves on the ground, for the three Sword nodes that change it:
 * <b>Combat Magnet</b> (it comes to you), <b>Butcher's Cut</b> (there is more of
 * it) and <b>Headhunter</b> (sometimes there is a head).
 *
 * <p>All three work the same way as {@link MinersMagnet}, and for the same
 * reason: a mob's drops do not exist yet when it dies. {@code LivingEntity.die}
 * spawns them during the death, so the kill is queued and the queue is drained
 * at the end of the same server tick, once every drop entity is really there.
 * Butcher's Cut runs before the magnet in that pass, so the extra steak is
 * pocketed rather than left behind.
 */
public final class CombatDrops {
	/** How far from the body a drop still counts as this kill's. */
	private static final double RADIUS = 2.5;

	/** Chance a head-dropping mob parts with its head for a Headhunter. */
	private static final float HEADHUNTER_CHANCE = 0.25F;

	private record Pending(ServerLevel level, Vec3 pos, ServerPlayer player, EntityType<?> victim,
	                       boolean magnet, boolean butcher, boolean headhunter) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private CombatDrops() {
	}

	/** Queues a kill if the killer owns anything that cares about it. */
	public static void onKill(ServerPlayer player, LivingEntity victim) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		boolean magnet = CombatPerks.owns(player, CombatPerks.COMBAT_MAGNET);
		boolean butcher = CombatPerks.owns(player, CombatPerks.BUTCHERS_CUT)
			&& player.getMainHandItem().is(ItemTags.SWORDS);
		boolean headhunter = CombatPerks.owns(player, CombatPerks.HEADHUNTER)
			&& CombatPerks.appliesTo(victim)
			&& headOf(victim.getType()) != null;
		if (magnet || butcher || headhunter) {
			PENDING.add(new Pending(level, victim.position(), player, victim.getType(),
				magnet, butcher, headhunter));
		}
	}

	/** Called at the end of every server tick, after the death has spawned its drops. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (Pending pending : PENDING) {
			ServerPlayer player = pending.player();
			if (player.isRemoved() || player.level() != pending.level()) {
				continue;
			}
			AABB box = AABB.ofSize(pending.pos(), RADIUS * 2, RADIUS * 2, RADIUS * 2);
			List<ItemEntity> drops = pending.level().getEntitiesOfClass(ItemEntity.class, box,
				entity -> entity.tickCount <= 1 && !entity.isRemoved());

			if (pending.butcher()) {
				for (ItemEntity drop : drops) {
					if (drop.getItem().has(DataComponents.FOOD)) {
						ItemStack stack = drop.getItem().copy();
						stack.grow(stack.getCount());
						drop.setItem(stack);
					}
				}
			}
			if (pending.headhunter() && pending.level().getRandom().nextFloat() < HEADHUNTER_CHANCE) {
				net.minecraft.world.item.Item head = headOf(pending.victim());
				if (head != null) {
					ItemEntity dropped = new ItemEntity(pending.level(),
						pending.pos().x, pending.pos().y, pending.pos().z, new ItemStack(head));
					pending.level().addFreshEntity(dropped);
					drops = new ArrayList<>(drops);
					drops.add(dropped);
				}
			}
			if (pending.magnet()) {
				for (ItemEntity drop : drops) {
					collect(player, drop);
				}
				for (ExperienceOrb orb : pending.level().getEntitiesOfClass(ExperienceOrb.class, box,
					orb -> !orb.isRemoved())) {
					orb.playerTouch(player);
				}
			}
		}
		PENDING.clear();
	}

	/**
	 * The head this mob type can drop. Only the four vanilla already has: a node
	 * that invented new heads would be a different feature, and the point here
	 * is that the wall of trophies finally fills up.
	 */
	private static net.minecraft.world.item.Item headOf(EntityType<?> type) {
		if (type == EntityTypes.ZOMBIE) {
			return Items.ZOMBIE_HEAD;
		}
		if (type == EntityTypes.SKELETON) {
			return Items.SKELETON_SKULL;
		}
		if (type == EntityTypes.WITHER_SKELETON) {
			return Items.WITHER_SKELETON_SKULL;
		}
		if (type == EntityTypes.CREEPER) {
			return Items.CREEPER_HEAD;
		}
		if (type == EntityTypes.PIGLIN) {
			return Items.PIGLIN_HEAD;
		}
		return null;
	}

	/** Same pickup bookkeeping as the Miner's Magnet, so advancements keep firing. */
	private static void collect(ServerPlayer player, ItemEntity drop) {
		ItemStack stack = drop.getItem();
		int before = stack.getCount();
		if (before <= 0) {
			return;
		}
		player.getInventory().add(stack);
		int taken = before - stack.getCount();
		if (taken <= 0) {
			return;
		}
		player.take(drop, taken);
		if (stack.isEmpty()) {
			drop.discard();
			stack.setCount(before);
		} else {
			drop.setItem(stack);
		}
		player.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), taken);
		player.onItemPickup(drop);
	}
}
