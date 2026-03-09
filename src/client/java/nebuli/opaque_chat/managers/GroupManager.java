package nebuli.opaque_chat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.GroupData;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;

public class GroupManager {
    private static final Path GROUPS_FILE = OpaqueChatClient.CONFIG_DIR.resolve("groups.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Map<String, List<String>> groupSessionHistory = new HashMap<>();

    public static Map<String, GroupData> groups = new HashMap<>();

    public static void loadGroups() {
        try {
            if (Files.exists(GROUPS_FILE)) {
                String jsonInput = Files.readString(GROUPS_FILE);

                Type type = new TypeToken<Map<String, GroupData>>() {
                }.getType();
                groups = GSON.fromJson(jsonInput, type);

                if (groups == null) {
                    groups = new HashMap<>();
                }

                OpaqueChatClient.LOGGER.info("[Opaque Chat] Loaded {} groups.", groups.size());
            } else {
                saveGroups();
            }
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to load groups.json", e);
        }
    }

    public static void saveGroups() {
        try {
            String jsonOutput = GSON.toJson(groups);
            Files.writeString(GROUPS_FILE, jsonOutput);
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to save groups.json", e);
        }
    }

    public static void addGroup(GroupData newGroup) {
        groups.put(newGroup.uuid, newGroup);
        saveGroups();
        OpaqueChatClient.LOGGER.info("[Opaque Chat] Saved new group: {}", newGroup.name);
    }

    public static GroupData createGroup(String groupName, List<String> members) {
        String groupUuid = UUID.randomUUID().toString();

        byte[] rawAesKey = new byte[16];
        new SecureRandom().nextBytes(rawAesKey);
        String base64Key = Base64.getEncoder().encodeToString(rawAesKey);

        GroupData newGroup = new GroupData(groupUuid, groupName, base64Key, members, 1);
        addGroup(newGroup);

        return newGroup;
    }

    public static void removeGroup(String groupUuid) {
        if (groups.containsKey(groupUuid)) {
            groups.remove(groupUuid);
            groupSessionHistory.remove(groupUuid);
            saveGroups();
        }
    }
}