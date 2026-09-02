package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The hoe passives that act on what a harvested crop drops. One end-of-tick pass,
 * for the same reason {@link AxeHarvest} has one: they compose in a fixed order
 * and none of them may touch the same drop twice.
 *
 *   Full Ears          — Fortune on the hoe multiplies the wheat, not only the seeds
 *   Gilded Roots I/II  — 5% / 10% for a harvested carrot to come out golden
 *   Clean Crop I/II    — the poisonous potato roll is halved, then all but removed
 *   Green Thumb        — a fully grown crop is put straight back in the ground
 *   Harvester's Magnet — whatever is left goes into the inventory
 *
 * <p><b>Why replanting happens here and not at break time.</b> Taking the seed out
 * of the inventory fails on the very first harvest of a session: the seed is still
 * an item on the floor, and with a magnet it is <em>still</em> on the floor at
 * break time, because the magnet also runs at the end of the tick. So the seed is
 * taken out of the drops this harvest just produced, before the magnet pass. That
 * works with an empty inventory, with or without a magnet, and leaves the player
 * with exactly the same n-1 seeds either way. The inventory scan survives only as
 * a fallback, for a crop that dropped no seed at all.
 */
public final class HoeHarvest {
	/**
	 * Tighter than {@link AxeHarvest}'s 1.5: Harvest Swing breaks crops one block
	 * apart, and a box that wide would claim four neighbours' drops.
	 */
	private static final double DROP_RADIUS = 0.75;

	private record Pending(ServerLevel level, UUID playerId, BlockPos pos, Block crop,
	                       boolean replant, int wheatFortune, int goldenCarrotPercent,
	                       int poisonCullPercent, boolean magnet) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private HoeHarvest() {
	}

	public static void onBreak(Level level, Player player, BlockPos pos, BlockState state) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		ItemStack hoe = serverPlayer.getMainHandItem();
		if (!hoe.is(ItemTags.HOES) || !HoeCrops.isHarvestable(state)) {
			return;
		}

		boolean replant = PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "green_thumb");
		int wheatFortune = state.is(Blocks.WHEAT)
			&& PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "full_ears")
			? fortuneLevel(serverPlayer, hoe)
			: 0;
		int golden = state.is(Blocks.CARROTS)
			? switch (PerkAccess.rank(serverPlayer, SkillTrees.GROUND, "gilded_roots_1", "gilded_roots_2")) {
				case 1 -> 5;
				case 2 -> 10;
				default -> 0;
			}
			: 0;
		int poison = state.is(Blocks.POTATOES)
			? switch (PerkAccess.rank(serverPlayer, SkillTrees.GROUND, "clean_crop_1", "clean_crop_2")) {
				case 1 -> 50;
				case 2 -> 90;
				default -> 0;
			}
			: 0;
		boolean magnet = PerkAccess.owns(serverPlayer, SkillTrees.GROUND, "harvesters_magnet");

		if (!replant && wheatFortune == 0 && golden == 0 && poison == 0 && !magnet) {
			return;
		}
		PENDING.add(new Pending(serverLevel, serverPlayer.getUUID(), pos, state.getBlock(),
			replant, wheatFortune, golden, poison, magnet));
	}

	/** Called at the end of every server tick, once every drop of the tick has spawned. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		Set<Integer> claimed = new HashSet<>();

		for (Pending pending : PENDING) {
			RandomSource random = pending.level().getRandom();
			List<ItemEntity> drops = new ArrayList<>();
			for (ItemEntity candidate : pending.level().getEntitiesOfClass(
				ItemEntity.class, new AABB(pending.pos()).inflate(DROP_RADIUS), entity -> entity.tickCount <= 1)) {
				if (claimed.add(candidate.getId())) {
					drops.add(candidate);
				}
			}

			List<ItemEntity> bonus = new ArrayList<>();
			// Full Ears is exactly vanilla's uniform_bonus_count with a multiplier
			// of one — the roll the seeds have always had, now on the wheat too.
			if (pending.wheatFortune() > 0) {
				for (ItemEntity drop : drops) {
					ItemStack stack = drop.getItem();
					if (!stack.is(Items.WHEAT)) {
						continue;
					}
					int extra = random.nextInt(pending.wheatFortune() + 1);
					if (extra > 0) {
						bonus.add(spawn(pending.level(), drop.blockPosition(), stack.copyWithCount(extra)));
					}
				}
			}
			// Gilded Roots rolls once per crop harvested, not once per carrot:
			// Fortune has already multiplied the stack, and rolling per item would
			// quadruple the intended rate on a Fortune III hoe.
			if (pending.goldenCarrotPercent() > 0 && random.nextInt(100) < pending.goldenCarrotPercent()) {
				bonus.add(spawn(pending.level(), pending.pos(), new ItemStack(Items.GOLDEN_CARROT)));
			}
			// A bonus drop is fresh, so the next pending break in this same pass
			// would otherwise find it and treat it as its own.
			for (ItemEntity extraDrop : bonus) {
				claimed.add(extraDrop.getId());
			}
			drops.addAll(bonus);

			// Clean Crop culls the entity rather than fighting the loot table, so it
			// composes with Harvest Swing for free.
			if (pending.poisonCullPercent() > 0) {
				for (ItemEntity drop : drops) {
					if (drop.getItem().is(Items.POISONOUS_POTATO)
						&& random.nextInt(100) < pending.poisonCullPercent()) {
						drop.discard();
					}
				}
			}

			ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
			if (player == null) {
				continue;
			}
			// Before the magnet, so the seed is still on the ground to pay with.
			if (pending.replant()) {
				replant(player, pending.level(), pending.pos(), pending.crop(), drops);
			}
			if (pending.magnet()) {
				for (ItemEntity drop : drops) {
					if (!drop.isRemoved()) {
						drop.setNoPickUpDelay();
						drop.playerTouch(player); // handles partial pickup, sound and animation
					}
				}
			}
		}
		PENDING.clear();
	}

	/**
	 * Green Thumb. Pays for the replant out of the harvest itself, falling back on
	 * the bag only when the crop dropped no seed at all — see the class note.
	 */
	private static void replant(ServerPlayer player, ServerLevel level, BlockPos pos,
			Block crop, List<ItemEntity> drops) {
		Item seed = HoeCrops.seedFor(crop);
		if (seed == null) {
			return;
		}
		BlockState fresh = crop.defaultBlockState();
		// Farmland can be trampled back to dirt in the same tick the crop broke,
		// and planting on dirt would pop the crop straight off again.
		if (!level.getBlockState(pos).isAir() || !fresh.canSurvive(level, pos)) {
			return;
		}
		for (ItemEntity drop : drops) {
			ItemStack stack = drop.getItem();
			if (drop.isRemoved() || !stack.is(seed)) {
				continue;
			}
			stack.shrink(1);
			if (stack.isEmpty()) {
				drop.discard();
			} else {
				drop.setItem(stack);
			}
			plant(player, level, pos, fresh);
			return;
		}
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack inSlot = inventory.getItem(slot);
			if (inSlot.is(seed)) {
				inSlot.shrink(1);
				plant(player, level, pos, fresh);
				return;
			}
		}
	}

	private static void plant(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState fresh) {
		level.setBlockAndUpdate(pos, fresh);
		SkillService.addCount(player, SkillTrees.GROUND, "replant_with_green_thumb", 1);
	}

	private static int fortuneLevel(ServerPlayer player, ItemStack stack) {
		if (!stack.isEnchanted()) {
			return 0;
		}
		Holder<Enchantment> fortune = player.level().registryAccess()
			.lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FORTUNE).orElse(null);
		return fortune == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(fortune, stack);
	}

	private static ItemEntity spawn(ServerLevel level, BlockPos pos, ItemStack stack) {
		ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
		entity.setDefaultPickUpDelay();
		level.addFreshEntity(entity);
		return entity;
	}
}
