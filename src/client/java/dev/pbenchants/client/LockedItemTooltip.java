package dev.pbenchants.client;

import dev.pbenchants.perk.ArmorPerks;
import dev.pbenchants.perk.ItemAuthority;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

/**
 * The line that stops a degraded tool from reading as a bug.
 *
 * <p>Three shapes, one rule. On a <b>tool</b> the holder has not earned, it
 * says the item handles like bare hands and names the rank to go and unlock. On
 * a piece of <b>armour</b> it says the piece protects like bare skin. On an
 * <b>enchanted book</b> — the librarian's offer — it says the trade is refused
 * until then. Either way the tooltip doubles as advertising for a tier the
 * player has not reached, which is the same job the villager's offer does.
 *
 * <p>One rule for both: the rank on the label is the rank you have to own.
 * A Dig Range III pickaxe warns a rank II player exactly as a Dig Range III
 * book does, because neither is any use to them.
 */
public final class LockedItemTooltip {
	private LockedItemTooltip() {
	}

	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			var player = Minecraft.getInstance().player;
			if (player == null) {
				return;
			}
			boolean book = stack.is(Items.ENCHANTED_BOOK);
			ItemAuthority.Unmet unmet = ItemAuthority.firstUnmet(player, stack);
			if (unmet == null) {
				return;
			}
			String key = book ? "item.pbenchants.locked.book"
				: ArmorPerks.isArmour(stack) ? "item.pbenchants.locked.armor"
				: "item.pbenchants.locked.tool";
			lines.add(Component.translatable(key, unmet.name()).withStyle(ChatFormatting.RED));
		});
	}
}
