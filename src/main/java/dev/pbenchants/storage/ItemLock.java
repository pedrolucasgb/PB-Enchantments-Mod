package dev.pbenchants.storage;

import dev.pbenchants.PBEnchants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The two marks an Artisan can put on a stack. Both are data components on
 * the item itself, so they are saved with it, synced with it and copied with
 * it, and neither needs any progress state.
 *
 * <p><b>Locked Items</b> — the Artisan's safety rail, Terraria's "favourite"
 * by another name. Alt-click a stack and it wears a mark; from then on
 * nothing the mod does by itself will move it (Sort, Quick Stack, Restock,
 * Deft Hands, Auto Block), and no <em>shortcut</em> will take it out of your
 * inventory (a shift-click into a chest, the drop key). Your own hand still
 * moves it wherever you like — and the mark goes with it, because it is on
 * the item, not on the slot it happened to sit in.
 *
 * <p><b>Void Mark</b> — the capstone tier's other half, for the player whose
 * bag fills with cobblestone faster than any chest empties it. Alt +
 * right-click a stack and it becomes a filter: while it is in your
 * inventory, every item of that kind you pick up — by hand or by any of the
 * magnets — is destroyed on the spot instead of taking a slot
 * ({@link VoidMark}). The marked stack itself is left exactly as it is, and
 * the mod's automatic movers step around it the way they step around a
 * locked one, so the filter stays where you put it.
 *
 * <p>Until 0.8.5 the lock was a bitmask of <b>slots</b> on the Artisan tree,
 * which had a flaw you met the first time you rearranged your bag: the pin
 * stayed behind on the empty slot, and the pickaxe you meant to protect
 * walked off unmarked. A component has neither problem.
 *
 * <p>A component would normally make a marked stack a different kind of stack
 * from a plain one — and a locked pile of dirt that refused the dirt you dig
 * up is not a lock, it is a nuisance. So {@code ItemStackMixin} makes both
 * marks invisible to {@code isSameItemSameComponents}: more of the same item
 * still stacks into a locked pile until it is full, the pile keeps its mark
 * (vanilla grows the stack that was already there), and only what overflows
 * starts a plain, unmarked stack of its own.
 *
 * <p>The component ids live under {@link PBEnchants#DATA_NS}: they are written
 * into item data a world keeps, like every other persisted id of the mod.
 */
public final class ItemLock {
	/** The Artisan node that grants the alt-click. Id kept from the slot-lock days, for saves. */
	public static final String NODE = "slot_lock";

	/** The Artisan tier-5 node that grants the alt + right-click. */
	public static final String VOID_NODE = "void_mark";

	public static final DataComponentType<Unit> LOCKED = DataComponentType.<Unit>builder()
		.persistent(Unit.CODEC)
		.networkSynchronized(Unit.STREAM_CODEC)
		.build();

	public static final DataComponentType<Unit> VOIDED = DataComponentType.<Unit>builder()
		.persistent(Unit.CODEC)
		.networkSynchronized(Unit.STREAM_CODEC)
		.build();

	private ItemLock() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, PBEnchants.DATA_NS + ":locked", LOCKED);
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, PBEnchants.DATA_NS + ":voided", VOIDED);
	}

	public static boolean locked(ItemStack stack) {
		return !stack.isEmpty() && stack.has(LOCKED);
	}

	public static boolean voided(ItemStack stack) {
		return !stack.isEmpty() && stack.has(VOIDED);
	}

	/** Either mark: the stack is one the player put a deliberate hand on. */
	public static boolean marked(ItemStack stack) {
		return !stack.isEmpty() && (stack.has(LOCKED) || stack.has(VOIDED));
	}

	/**
	 * True when the mod's own movers — Sort, Quick Stack, Restock, Deft Hands,
	 * Auto Block — must leave this stack where it is. A lock says so
	 * outright; a void mark says so because the filter only works while the
	 * marked stack stays in the bag, and a sort that carried it into a chest
	 * would switch the filter off without a word.
	 */
	public static boolean held(ItemStack stack) {
		return marked(stack);
	}

	public static void setLocked(ItemStack stack, boolean locked) {
		if (locked) {
			stack.set(LOCKED, Unit.INSTANCE);
		} else {
			stack.remove(LOCKED);
		}
	}

	public static void setVoided(ItemStack stack, boolean voided) {
		if (voided) {
			stack.set(VOIDED, Unit.INSTANCE);
		} else {
			stack.remove(VOIDED);
		}
	}

	/** A copy of the stack with both marks taken off — what it is, without what the player said about it. */
	public static ItemStack bare(ItemStack stack) {
		ItemStack copy = stack.copy();
		copy.remove(LOCKED);
		copy.remove(VOIDED);
		return copy;
	}

	/**
	 * The one line a refused shortcut gets. Server-side only: the mixins that
	 * refuse run on both sides, and the client's half is a prediction that
	 * should say nothing of its own.
	 */
	public static void refused(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("msg.pbenchants.item_lock.kept"), true);
		}
	}
}
