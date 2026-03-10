package nebuli.opaque_chat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import nebuli.opaque_chat.OpaqueChatClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private static final Path CONFIG_FILE = OpaqueChatClient.CONFIG_DIR.resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ModConfig {
        public static int outer_gui_button_width = 22;
        public static int outer_gui_button_height = 20;
        public static int inner_gui_button_y = 20;
        public static int inner_gui_button_width = 100;
        public static int inner_gui_button_height = 20;
        public static int chat_input_height = 12;
        public static int contact_column_width = 140;
        public static int search_input_height = 16;
        public static int panel_distance_from_top = 0;

        public static int contact_entry_height = 14;

        public static double animation_duration = 350f;

        public static int contacts_bg = 0x22111111;
        public static int chat_area_bg = 0x36444444;
        public static int outline_color = 0xFF555555;
        public static int prompt_color = 0xFFAAAAAA;

        public static int contact_selected_bg = 0x55FFFFFF;
        public static int contact_hover_bg = 0x22FFFFFF;
        public static int contact_online_icon = 0xFF55FF55;
        public static int contact_offline_icon = 0xFFAAAAAA;
        public static int text_primary = 0xFFFFFFFF;
        public static int text_hover = 0xFFFFFFAA;

        public static Map<String, Integer> server_cooldowns = new HashMap<>();
        public static int server_chat_cooldown = 5000;

        public static String format_code_modname = "d";
        public static String format_code_highlight = "b";
        public static String format_code_info = "e";
        public static String format_code_sender = "a";
        public static String format_code_unhighlight1 = "8";
        public static String format_code_unhighlight2 = "7";
        public static String format_code_error = "c";
        public static String format_code_success = "a";

        public static int group_creation_columns = 3;

        public static java.util.List<String> blocked_players = new java.util.ArrayList<>();
    }

    public static ModConfig config = new ModConfig();

    public static void loadConfig() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                config = GSON.fromJson(Files.readString(CONFIG_FILE), ModConfig.class);
                if (config == null) config = new ModConfig();
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to load config", e);
        }
    }

    public static void saveConfig() {
        try {
            Files.writeString(CONFIG_FILE, GSON.toJson(config));
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to save config", e);
        }
    }
}