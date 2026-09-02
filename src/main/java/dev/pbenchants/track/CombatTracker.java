package dev.pbenchants.track;

import dev.pbenchants.perk.CombatPerks;
import dev.pbenchants.progress.TreeProgress;
import dev.pbenchants.skill.GateChecklists;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Feeds the gates of the Sword tree. Everything the class asks for is about a
 * fight, so almost all of it hangs off two events — a hit landing and a mob
 * dying — plus a once-a-second sweep for the two facts that are about the
 * player rather than the swing (owning a trident, having survived a raid).
 *
 * <p>Counters here are <b>cumulative</b>, like every other tree in the mod: the
 * tier-2 target of 500 kills already contains the 200 that bought tier 1.
 */
public final class CombatTracker {
	/** Health fraction below which a kill counts as a desperate one. */
	private static final float DESPERATE = 0.3F;

	private CombatTracker() {
	}

	private static TreeProgress progress(ServerPlayer player) {
		return SkillService.progress(player, SkillTrees.SWORD);
	}

	/**
	 * Melee damage the player actually dealt — the target's health before the
	 * blow minus after, read where vanilla reads its own DAMAGE_DEALT stat.
	 * Whiffed clicks, invulnerability frames and overkill past the last heart
	 * all count for what they were worth: nothing.
	 */
	public static void onMeleeDamage(ServerPlayer player, float amount) {
		if (amount > 0.0F) {
			progress(player).addCount("melee_damage", Math.round(amount));
		}
	}

	/** One sweep hit — the tier-3 gate counts the arc, not the kill. */
	public static void onSweepHit(ServerPlayer player) {
		progress(player).addCount("sweep_hits", 1);
	}

	/** A mace hit that converted a fall of five blocks or more. */
	public static void onMaceSlam(ServerPlayer player) {
		progress(player).addCount("mace_slams", 1);
	}

	/**
	 * A mob died to this player. Hostile-only for the count — the class is about
	 * fighting things that fight back, and a tree that levelled off cows would
	 * pace itself in a pen.
	 */
	public static void onKill(ServerPlayer player, LivingEntity victim) {
		TreeProgress progress = progress(player);
		boolean hostile = victim instanceof Enemy;
		if (hostile) {
			progress.addCount("kill_hostiles", 1);
			progress.see("mob", BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString(), "mob_checklist");
			if (player.level().dimension() == Level.NETHER) {
				progress.addCount("nether_kills", 1);
			}
			if (player.getHealth() / player.getMaxHealth() < DESPERATE) {
				progress.addCount("desperate_kills", 1);
			}
			if (CombatPerks.state(player).critTarget == victim.getId()) {
				progress.addCount("crit_kills", 1);
			}
		}
		if (victim.getType() == EntityTypes.WITHER || victim.getType() == EntityTypes.ELDER_GUARDIAN) {
			progress.counters.put("slay_boss", 1);
		}
		if (victim.getType() == EntityTypes.ENDER_DRAGON) {
			progress.counters.put("slay_dragon", 1);
		}
		int bossBit = bossBit(victim);
		if (bossBit >= 0) {
			GateChecklists.tick(progress, "boss_checklist", bossBit);
		}
	}

	/**
	 * Once a second: the two gates that are a state rather than an event.
	 *
	 * <p>"Survive a raid" is read off Hero of the Village — you only ever have
	 * that effect for having lived through one, and asking the raid system
	 * directly would mean hooking a system this tree has nothing else to do
	 * with.
	 */
	public static void tick(ServerPlayer player) {
		TreeProgress progress = progress(player);
		if (progress.count("own_trident") < 1 && holdsTrident(player)) {
			progress.counters.put("own_trident", 1);
		}
		if (progress.count("survive_raid") < 1 && player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
			progress.counters.put("survive_raid", 1);
		}
	}

	/**
	 * Which line of the boss checklist this kill ticks, or -1 for anything
	 * else. The roster is {@link GateChecklists}'; this only says which entity
	 * is which bit.
	 */
	private static int bossBit(LivingEntity victim) {
		EntityType<?> type = victim.getType();
		if (type == EntityTypes.ELDER_GUARDIAN) {
			return 0;
		}
		if (type == EntityTypes.WITHER) {
			return 1;
		}
		if (type == EntityTypes.WARDEN) {
			return 2;
		}
		if (type == EntityTypes.ENDER_DRAGON) {
			return 3;
		}
		return -1;
	}

	private static boolean holdsTrident(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.is(Items.TRIDENT)) {
				return true;
			}
		}
		return false;
	}
}
