package dev.pbenchants.perk;

import dev.pbenchants.progress.ModAttachments;
import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The Explorer's ender chest, in two steps.
 *
 * <p><b>Portable Ender Chest</b> (tier 4): the chest opens from anywhere — the
 * mini chest button on the inventory screen, or {@code /echest}. It is exactly
 * the container vanilla would show over the block, so there is nothing to
 * migrate and nothing to desync: the screen is a window, not a copy.
 *
 * <p><b>Double Ender Chest</b> (tier 5): the same chest, six rows. The first
 * 27 slots remain vanilla's own {@code EnderItems} — untouched on disk, still
 * there if the node is sold or the mod removed. The second 27 are an annex the
 * mod keeps on the player ({@link ModAttachments#ENDER_ANNEX}), written back
 * on every change the way Shulker Sight writes its box, and carried across
 * death like the rest of the player's progress. Once earned, the doubling
 * applies everywhere the chest opens: the button, the command, and the placed
 * block itself.
 */
public final class EnderChestAccess {
	public static final String PORTABLE_NODE = "portable_ender_chest";
	public static final String DOUBLE_NODE = "double_ender_chest";

	private static final int ANNEX_SIZE = 27;

	private EnderChestAccess() {
	}

	/** The button and the command: nothing under the crosshair required. */
	public static boolean open(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, PORTABLE_NODE)) {
			return false;
		}
		openMenu(player);
		return true;
	}

	/**
	 * The placed block, for a player who earned the doubling: vanilla's menu
	 * shows three rows and has never heard of the annex, so the mod takes the
	 * click. Anyone else falls through to vanilla untouched.
	 */
	public static boolean openFromBlock(ServerPlayer player) {
		if (!SkillService.owns(player, SkillTrees.EXPLORER, DOUBLE_NODE)) {
			return false;
		}
		openMenu(player);
		return true;
	}

	private static void openMenu(ServerPlayer player) {
		Component title = Component.translatable("container.enderchest");
		if (SkillService.owns(player, SkillTrees.EXPLORER, DOUBLE_NODE)) {
			player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> ChestMenu.sixRows(id, inventory,
					new CompoundContainer(p.getEnderChestInventory(), new Annex((ServerPlayer) p))),
				title));
		} else {
			player.openMenu(new SimpleMenuProvider(
				(id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
				title));
		}
	}

	/** Rows four to six, backed by the player attachment on every change. */
	private static final class Annex extends SimpleContainer {
		private final ServerPlayer player;

		Annex(ServerPlayer player) {
			super(ANNEX_SIZE);
			this.player = player;
			player.getAttachedOrElse(ModAttachments.ENDER_ANNEX, ItemContainerContents.EMPTY)
				.copyInto(getItems());
		}

		@Override
		public void setChanged() {
			super.setChanged();
			player.setAttached(ModAttachments.ENDER_ANNEX, ItemContainerContents.fromItems(getItems()));
		}
	}
}
