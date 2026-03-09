package nebuli.opaque_chat.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.util.Util;

import java.util.LinkedList;
import java.util.Queue;

public class MessageQueueManager {
    private static final Queue<String> outgoingQueue = new LinkedList<>();
    private static long lastMessageTime = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (outgoingQueue.isEmpty() || client.player == null) return;

            long currentTime = Util.getMeasuringTimeMs();
            long cooldown = getCurrentServerCooldown();

            if (currentTime - lastMessageTime >= cooldown) {
                String payload = outgoingQueue.poll();
                client.player.networkHandler.sendChatMessage(payload);
                lastMessageTime = currentTime;
            }
        });
    }

    public static void enqueueMessage(String message) {
        outgoingQueue.add(message);
    }

    public static int getCurrentServerCooldown() {
        String serverIp = getCurrentServerId();

        if (serverIp == null) {
            return ConfigManager.ModConfig.server_chat_cooldown;
        }

        return ConfigManager.ModConfig.server_cooldowns.getOrDefault(serverIp, ConfigManager.ModConfig.server_chat_cooldown);
    }

    public static String getCurrentServerId() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getCurrentServerEntry() == null) {
            return null;
        }

        String rawAddress = client.getCurrentServerEntry().address;

        ServerAddress address = ServerAddress.parse(rawAddress);

        return address.getAddress().toLowerCase() + ":" + address.getPort();
    }
}