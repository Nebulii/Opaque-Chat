package nebuli.opaque_chat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.ContactData;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactManager {
    private static final Path CONTACTS_FILE = OpaqueChatClient.CONFIG_DIR.resolve("contacts.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Map<String, List<String>> sessionHistory = new HashMap<>();

    public static Map<String, ContactData> contacts = new HashMap<>();

    public static void loadContacts() {
        try {
            if (Files.exists(CONTACTS_FILE)) {
                String jsonInput = Files.readString(CONTACTS_FILE);

                Type type = new TypeToken<Map<String, ContactData>>(){}.getType();
                contacts = GSON.fromJson(jsonInput, type);

                if (contacts == null) {
                    contacts = new HashMap<>();
                }

                OpaqueChatClient.LOGGER.info("[Opaque Chat] Loaded " + contacts.size() + " contacts.");
            } else {
                saveContacts();
            }
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to load contacts.json", e);
        }
    }

    public static void saveContacts() {
        try {
            String jsonOutput = GSON.toJson(contacts);
            Files.writeString(CONTACTS_FILE, jsonOutput);
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to save contacts.json", e);
        }
    }

    public static void addContact(ContactData newContact) {
        contacts.put(newContact.username.toLowerCase(), newContact);
        saveContacts();
        OpaqueChatClient.LOGGER.info("[Opaque Chat] Saved new contact: " + newContact.username);
    }
}