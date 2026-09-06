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
 * Locked Items — the Artisan's safety rail, Terraria's "favourite" by another
 * name. Alt-click a stack and it wears a mark; from then on nothing the mod
 * does by itself will move it (Sort, Quick Stack, Restock, Deft Hands, Auto
 * Block), and no <em>shortcut</em> will take it out of your inventory (a
 * shift-click into a chest, the drop key). Your own hand still moves it
 * wherever you like — and the mark goes with it, because it is on the item,
 * not on the slot it happened to sit in.
 *
 * <p>Until 0.8.5 this was a bitmask of <b>slots</b> on the Artisan tree, which
 * had a flaw you met the first time you rearranged your bag: the pin stayed
 * behind on the empty slot, and the pickaxe you meant to protect walked off
 * unmarked. A data component on the stack has neither problem — it is saved
 * with the item, synced to the client with the item, and copied when the item
 * is copied — and it needs no progress state at all.
 *
 * <p>One consequence worth knowing: a marked stack is a different stack from
 * an unmarked one of the same item, so the two never merge. Pick up more dirt
 * and it starts its own pile next to the locked one, which is exactly what a
 * lock should mean.
 *
 * <p>The component id lives under {@link PBEnchants#DATA_NS}: it is written
 * into item data a world keeps, like every other persisted id of the mod.
 */
public final class ItemLock {
	/** The Artisan node that grants the alt-click. Id kept from the slot-lock days, for saves. */
	public static final String NODE = "slot_lock";

	public static final DataComponentType<Unit> LOCKED = DataComponentType.<Unit>builder()
		.persistent(Unit.CODEC)
		.networkSynchronized(Unit.STREAM_CODEC)
		.build();

	private ItemLock() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, PBEnchants.DATA_NS + ":locked", LOCKED);
	}

	public static boolean locked(ItemStack stack) {
		return !stack.isEmpty() && stack.has(LOCKED);
	}

	public static void setLocked(ItemStack stack, boolean locked) {
		if (locked) {
			stack.set(LOCKED, Unit.INSTANCE);
		} else {
			stack.remove(LOCKED);
		}
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
