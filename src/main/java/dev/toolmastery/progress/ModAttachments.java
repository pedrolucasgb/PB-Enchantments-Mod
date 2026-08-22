package dev.toolmastery.progress;

import dev.toolmastery.ToolMastery;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ModAttachments {
	public static final AttachmentType<PlayerProgress> PROGRESS = AttachmentRegistry.create(
		Identifier.fromNamespaceAndPath(ToolMastery.MOD_ID, "progress"),
		builder -> builder
			.initializer(PlayerProgress::new)
			.persistent(PlayerProgress.CODEC)
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
