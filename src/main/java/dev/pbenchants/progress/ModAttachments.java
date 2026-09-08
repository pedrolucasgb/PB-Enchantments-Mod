package dev.pbenchants.progress;

import dev.pbenchants.PBEnchants;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ModAttachments {
	public static final AttachmentType<PlayerProgress> PROGRESS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, "progress"),
		builder -> builder
			.initializer(PlayerProgress::new)
			.persistent(PlayerProgress.CODEC)
			.copyOnDeath()
	);

	/**
	 * Double Ender Chest: the second 27 slots. The first 27 stay in vanilla's
	 * own {@code EnderItems} so turning the node off (or removing the mod)
	 * loses nothing a vanilla player could see; only the annex is ours.
	 */
	public static final AttachmentType<ItemContainerContents> ENDER_ANNEX = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(PBEnchants.DATA_NS, "ender_annex"),
		builder -> builder
			.initializer(() -> ItemContainerContents.EMPTY)
			.persistent(ItemContainerContents.CODEC)
			.copyOnDeath()
	);

	private ModAttachments() {
	}

	/** Called from mod init so the static registration runs at startup. */
	public static void init() {
	}

	public static PlayerProgress of(ServerPlayer player) {
		return player.getAttachedOrCreate(PROGRESS);
	}
}
