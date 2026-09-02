package dev.pbenchants.network;

import dev.pbenchants.PBEnchants;
import net.fabricmc.fabric.api.networking.v1.FabricServerConfigurationPacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;

import java.util.function.Consumer;

/**
 * Refuses any client whose PB Enchantments does not match the server's, while
 * the connection is still in the configuration phase — before the player
 * exists, before any play payload could arrive from a client that would
 * decode it differently.
 *
 * <p>Two ways in, two answers. A client without the mod at all (vanilla, or a
 * different mod list) never registered the {@code pbenchants:version_check}
 * channel, so {@code canSend} says no and it is turned away by name. A client
 * with the mod gets a {@link ConfigurationTask} that vanilla will not finish
 * the login without: the server states its version, the client answers with
 * its own, and only an exact match completes the task. Anything else is
 * disconnected with both versions in the message, so the player knows which
 * side is behind.
 */
public final class VersionGate {
	/** Vanilla waits on this task; only a matching version reply completes it. */
	private static final ConfigurationTask.Type TASK =
		new ConfigurationTask.Type(PBEnchants.MOD_ID + ":version_check");

	private VersionGate() {
	}

	/** The version baked into whichever jar this code is running from. */
	public static String modVersion() {
		return FabricLoader.getInstance().getModContainer(PBEnchants.MOD_ID).orElseThrow()
			.getMetadata().getVersion().getFriendlyString();
	}

	public static void init() {
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (!ServerConfigurationNetworking.canSend(handler, VersionCheckPayload.TYPE)) {
				handler.disconnect(Component.literal(
					"PB Enchantments " + modVersion() + " is required to join this server."));
				return;
			}
			((FabricServerConfigurationPacketListenerImpl) handler).addTask(new ConfigurationTask() {
				@Override
				public void start(Consumer<Packet<?>> sender) {
					sender.accept(ServerConfigurationNetworking.createClientboundPacket(
						new VersionCheckPayload(modVersion())));
				}

				@Override
				public ConfigurationTask.Type type() {
					return TASK;
				}
			});
		});

		ServerConfigurationNetworking.registerGlobalReceiver(VersionReplyPayload.TYPE, (payload, context) -> {
			String server = modVersion();
			if (server.equals(payload.version())) {
				((FabricServerConfigurationPacketListenerImpl) context.packetListener()).completeTask(TASK);
			} else {
				context.packetListener().disconnect(Component.literal(
					"PB Enchantments version mismatch.\n"
						+ "Server: " + server + " — you: " + payload.version() + "\n"
						+ "Update your mod to " + server + " and try again."));
			}
		});
	}
}
