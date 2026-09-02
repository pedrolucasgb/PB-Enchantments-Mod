package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gravedigger — you take no fall damage landing in a hole your own shovel opened.
 *
 * <p>Insurance on your own work, not on your judgement: somebody else's pit, a
 * ravine and a missed jump all hurt exactly as much as they always did. What
 * counts is a shaft in the column you are standing in, dug by you, recently — so
 * the perk answers the mistake the class actually makes, which is digging down
 * one block too many with Flat Earth or the aura running.
 *
 * <p>Kept in memory only. A dug column is worth remembering for the half a minute
 * it takes to fall down it and no longer, and a save that carried a list of every
 * hole a player ever made would be a lot of bytes for a rescue that has already
 * happened.
 */
public final class Gravedigger {
	public static final String NODE = "gravedigger";

	/** Long enough to fall down anything you can dig, short enough to forget. */
	private static final long MEMORY_TICKS = 600L; // 30 seconds

	/** Plenty for a Flat Earth column; keeps the scan on a fall trivial. */
	private static final int MAX_REMEMBERED = 256;

	private record Dug(BlockPos pos, long time) {
	}

	private static final Map<UUID, Deque<Dug>> RECENT = new HashMap<>();

	private Gravedigger() {
	}

	/** Every shovel break is a candidate; ownership is checked when it matters. */
	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!serverPlayer.getMainHandItem().is(ItemTags.SHOVELS)) {
			return;
		}
		if (!PerkAccess.owns(serverPlayer, SkillTrees.GROUND, NODE)) {
			return;
		}
		Deque<Dug> dug = RECENT.computeIfAbsent(serverPlayer.getUUID(), uuid -> new ArrayDeque<>());
		dug.addLast(new Dug(pos.immutable(), level.getGameTime()));
		while (dug.size() > MAX_REMEMBERED) {
			dug.removeFirst();
		}
	}

	/**
	 * Registered on {@code ALLOW_DAMAGE}: returning false calls the hit off.
	 * Only fall damage, only in a column this player hollowed out.
	 */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!source.is(DamageTypes.FALL) || !(entity instanceof ServerPlayer player)) {
			return true;
		}
		Deque<Dug> dug = RECENT.get(player.getUUID());
		if (dug == null || dug.isEmpty()) {
			return true;
		}
		if (!PerkAccess.owns(player, SkillTrees.GROUND, NODE)) {
			return true;
		}
		long now = player.level().getGameTime();
		dug.removeIf(entry -> now - entry.time() > MEMORY_TICKS);

		BlockPos landed = player.blockPosition();
		for (Dug entry : dug) {
			// Same column, and at or above where they came to rest: that is the
			// shaft they dug, not the cliff they walked off.
			if (entry.pos().getX() == landed.getX()
				&& entry.pos().getZ() == landed.getZ()
				&& entry.pos().getY() >= landed.getY()) {
				return false;
			}
		}
		return true;
	}

	public static void forget(@Nullable ServerPlayer player) {
		if (player != null) {
			RECENT.remove(player.getUUID());
		}
	}
}
