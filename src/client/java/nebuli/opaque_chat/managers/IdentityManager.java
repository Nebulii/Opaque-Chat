package nebuli.opaque_chat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.IdentityData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class IdentityManager {
    private static final Path IDENTITY_FILE = OpaqueChatClient.CONFIG_DIR.resolve("identity.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static IdentityData currentIdentity;

    public static void loadOrGenerateIdentity() {
        try {
            if (!Files.exists(IDENTITY_FILE)) {
                OpaqueChatClient.LOGGER.info("[Opaque Chat] No identity found. Generating new RSA keypair...");
                String[] keys = CryptoManager.generateKeys();

                if (keys != null) {
                    Session session = MinecraftClient.getInstance().getSession();
                    String username = session.getUsername();
                    String onlineUuid = session.getUuidOrNull() != null ? session.getUuidOrNull().toString() : "";
                    String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();

                    currentIdentity = new IdentityData(username, onlineUuid, offlineUuid, keys[0], keys[1]);

                    Files.writeString(IDENTITY_FILE, GSON.toJson(currentIdentity));
                    OpaqueChatClient.LOGGER.info("[Opaque Chat] Keys generated and saved successfully!");
                }
            } else {
                OpaqueChatClient.LOGGER.info("[Opaque Chat] Existing identity found. Loading keys...");
                currentIdentity = GSON.fromJson(Files.readString(IDENTITY_FILE), IdentityData.class);
            }
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to load or generate identity!", e);
        }
    }
}