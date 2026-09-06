package dev.pbenchants.client;

import dev.pbenchants.storage.ItemLock;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The line under a locked stack: what the gold frame means, and how to take it
 * off. On every screen the stack shows up in, not just the inventory, because
 * the mark travels with the item.
 */
public final class ItemLockTooltip {
	private ItemLockTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (ItemLock.locked(stack)) {
				lines.add(Component.translatable("item.pbenchants.item_lock.tip").withStyle(ChatFormatting.GOLD));
			}
		});
	}
}
