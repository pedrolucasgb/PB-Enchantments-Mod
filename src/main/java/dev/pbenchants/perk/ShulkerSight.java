package dev.pbenchants.perk;

import dev.pbenchants.skill.SkillService;
import dev.pbenchants.skill.SkillTrees;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Shulker Sight — a shulker box opens in your hand.
 *
 * <p>Right-click with a shulker box at nothing in particular, or sneak and
 * right-click anywhere (a plain click on a block still places it), and its
 * 27 slots open as if it stood on the ground. The screen is vanilla's own shulker screen; what is different is
 * where the contents live. They are read out of the item's
 * {@code container} component into a plain 27-slot container when the screen
 * opens, and written straight back into the same item every time a slot
 * changes — so there is no moment at which the box on your hip and the box on
 * your screen disagree, and a crash or a disconnect mid-rummage loses nothing.
 *
 * <p>Two things keep it honest. The box being rummaged is <em>identified</em>,
 * not just located: the menu holds the very stack object that was in the hand
 * and closes the moment the slot holds anything else, so a box that is
 * dropped, swapped, or sorted out from under the screen takes its screen with
 * it. And the slot it sits in refuses every click while the screen is open —
 * you cannot pick the box up, shift-click it, throw it, or hotkey-swap it
 * into the box it is, which is the one way a container inside itself could
 * ever come about. Vanilla's own rule that no shulker box goes inside another
 * is kept by the slots themselves.
 */
public final class ShulkerSight {
	public static final String NODE = "shulker_sight";

	private static final int SIZE = 27;

	private ShulkerSight() {
	}

	/**
	 * A right-click that missed every block. True when the click was ours,
	 * which on the client only means "do not also try to use the item" — the
	 * client returns false and lets the vanilla use packet reach the server,
	 * exactly as the other use-item perks do.
	 */
	public static boolean onUseItem(Player player, InteractionHand hand) {
		return open(player, hand);
	}

	/**
	 * A right-click that hit a block: only while sneaking, so that a plain
	 * click still places the box. Returning true cancels the placement.
	 */
	public static boolean onUseBlock(Player player, InteractionHand hand) {
		return player.isShiftKeyDown() && open(player, hand);
	}

	private static boolean open(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (!isShulkerBox(held) || !(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}
		if (!SkillService.owns(serverPlayer, SkillTrees.ARTISAN, NODE)) {
			return false;
		}
		int slot = hand == InteractionHand.MAIN_HAND
			? serverPlayer.getInventory().getSelectedSlot()
			: Inventory.SLOT_OFFHAND;
		serverPlayer.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new Menu(id, inventory, new Contents(inventory, slot, held)),
			held.getHoverName()));
		return true;
	}

	/** The item tag rather than the block class: every colour, and whatever a data pack adds to it. */
	public static boolean isShulkerBox(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ItemTags.SHULKER_BOXES);
	}

	/**
	 * The 27 slots, backed by the item. Every change is written straight back
	 * into the stack's component; validity is the stack still being the very
	 * object that sits in the slot it was opened from.
	 */
	private static final class Contents extends SimpleContainer {
		private final Inventory inventory;
		private final int slot;
		private final ItemStack source;

		Contents(Inventory inventory, int slot, ItemStack source) {
			super(SIZE);
			this.inventory = inventory;
			this.slot = slot;
			this.source = source;
			source.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(getItems());
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if (open()) {
				source.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
			}
		}

		@Override
		public boolean stillValid(Player player) {
			return player.getInventory() == inventory && open();
		}

		boolean open() {
			return inventory.getItem(slot) == source && isShulkerBox(source);
		}

		/** Whether a click on this menu slot would touch the box being rummaged. */
		boolean isSource(Slot menuSlot) {
			return menuSlot.container == inventory && menuSlot.getContainerSlot() == slot;
		}

		/** A number-key swap (button 0-8, or 40 for the offhand key) aimed at the box's own slot. */
		boolean isSourceHotkey(int button) {
			return button == slot;
		}
	}

	/** Vanilla's shulker menu, with the box's own slot made untouchable while it is open. */
	private static final class Menu extends ShulkerBoxMenu {
		private final Contents contents;

		Menu(int id, Inventory inventory, Contents contents) {
			super(id, inventory, contents);
			this.contents = contents;
		}

		@Override
		public void clicked(int slotId, int button, ContainerInput input, Player player) {
			if (slotId >= 0 && slotId < slots.size() && contents.isSource(slots.get(slotId))) {
				return;
			}
			if (input == ContainerInput.SWAP && contents.isSourceHotkey(button)) {
				return;
			}
			super.clicked(slotId, button, input, player);
		}
	}
}
