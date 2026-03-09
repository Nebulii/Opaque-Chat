package nebuli.opaque_chat.gui;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import nebuli.opaque_chat.managers.ConfigManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class YaclScreenBuilder {

    public static Screen build(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Opaque Chat Configuration"))

                // ==========================================
                // TAB 1: GUI LAYOUT
                // ==========================================
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("GUI Layout"))
                        .tooltip(Text.literal("Configure the sizes and positions of the UI elements"))

                        // Outer Buttons
                        .option(Option.<Integer>createBuilder().name(Text.literal("Outer Button Width"))
                                .binding(22, () -> ConfigManager.ModConfig.outer_gui_button_width, newVal -> ConfigManager.ModConfig.outer_gui_button_width = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 50).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Outer Button Height"))
                                .binding(20, () -> ConfigManager.ModConfig.outer_gui_button_height, newVal -> ConfigManager.ModConfig.outer_gui_button_height = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 50).step(1)).build())

                        // Inner Buttons
                        .option(Option.<Integer>createBuilder().name(Text.literal("Inner Button Y Position"))
                                .binding(20, () -> ConfigManager.ModConfig.inner_gui_button_y, newVal -> ConfigManager.ModConfig.inner_gui_button_y = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Inner Button Width"))
                                .binding(100, () -> ConfigManager.ModConfig.inner_gui_button_width, newVal -> ConfigManager.ModConfig.inner_gui_button_width = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 200).step(5)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Inner Button Height"))
                                .binding(20, () -> ConfigManager.ModConfig.inner_gui_button_height, newVal -> ConfigManager.ModConfig.inner_gui_button_height = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 50).step(1)).build())

                        // Input Fields & Panels
                        .option(Option.<Integer>createBuilder().name(Text.literal("Chat Input Height"))
                                .binding(12, () -> ConfigManager.ModConfig.chat_input_height, newVal -> ConfigManager.ModConfig.chat_input_height = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(8, 30).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Search Input Height"))
                                .binding(16, () -> ConfigManager.ModConfig.search_input_height, newVal -> ConfigManager.ModConfig.search_input_height = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 30).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Contact Column Width"))
                                .binding(140, () -> ConfigManager.ModConfig.contact_column_width, newVal -> ConfigManager.ModConfig.contact_column_width = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(80, 250).step(5)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Panel Distance From Top"))
                                .binding(0, () -> ConfigManager.ModConfig.panel_distance_from_top, newVal -> ConfigManager.ModConfig.panel_distance_from_top = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Contact List Entry Height"))
                                .binding(14, () -> ConfigManager.ModConfig.contact_entry_height, newVal -> ConfigManager.ModConfig.contact_entry_height = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 30).step(1)).build())
                        .option(Option.<Integer>createBuilder().name(Text.literal("Group Creation Columns"))
                                .binding(3, () -> ConfigManager.ModConfig.group_creation_columns, newVal -> ConfigManager.ModConfig.group_creation_columns = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 5).step(1)).build())
                        .build())


                // ==========================================
                // TAB 2: COLORS
                // ==========================================
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Colors"))
                        .tooltip(Text.literal("Customize the theme of Opaque Chat"))

                        // Backgrounds
                        .option(Option.<Color>createBuilder().name(Text.literal("Contacts Background"))
                                .binding(new Color(0x22111111, true), () -> new Color(ConfigManager.ModConfig.contacts_bg, true), newVal -> ConfigManager.ModConfig.contacts_bg = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Chat Area Background"))
                                .binding(new Color(0x36444444, true), () -> new Color(ConfigManager.ModConfig.chat_area_bg, true), newVal -> ConfigManager.ModConfig.chat_area_bg = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Outline Color"))
                                .binding(new Color(0xFF555555, true), () -> new Color(ConfigManager.ModConfig.outline_color, true), newVal -> ConfigManager.ModConfig.outline_color = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        // Interactive Elements
                        .option(Option.<Color>createBuilder().name(Text.literal("Contact Selected BG"))
                                .binding(new Color(0x55FFFFFF, true), () -> new Color(ConfigManager.ModConfig.contact_selected_bg, true), newVal -> ConfigManager.ModConfig.contact_selected_bg = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Contact Hover BG"))
                                .binding(new Color(0x22FFFFFF, true), () -> new Color(ConfigManager.ModConfig.contact_hover_bg, true), newVal -> ConfigManager.ModConfig.contact_hover_bg = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Contact Online Icon"))
                                .binding(new Color(0xFF55FF55, true), () -> new Color(ConfigManager.ModConfig.contact_online_icon, true), newVal -> ConfigManager.ModConfig.contact_online_icon = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Contact Offline Icon"))
                                .binding(new Color(0xFFAAAAAA, true), () -> new Color(ConfigManager.ModConfig.contact_offline_icon, true), newVal -> ConfigManager.ModConfig.contact_offline_icon = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())

                        // Text Colors
                        .option(Option.<Color>createBuilder().name(Text.literal("Prompt Color"))
                                .binding(new Color(0xFFAAAAAA, true), () -> new Color(ConfigManager.ModConfig.prompt_color, true), newVal -> ConfigManager.ModConfig.prompt_color = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Text Primary"))
                                .binding(new Color(0xFFFFFFFF, true), () -> new Color(ConfigManager.ModConfig.text_primary, true), newVal -> ConfigManager.ModConfig.text_primary = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .option(Option.<Color>createBuilder().name(Text.literal("Text Hover"))
                                .binding(new Color(0xFFFFFFAA, true), () -> new Color(ConfigManager.ModConfig.text_hover, true), newVal -> ConfigManager.ModConfig.text_hover = newVal.getRGB())
                                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(true)).build())
                        .build())

                // ==========================================
                // TAB 3: ADVANCED & FORMATTING
                // ==========================================
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Advanced"))

                        // Mechanics
                        .option(Option.<Integer>createBuilder().name(Text.literal("Server Chat Cooldown (ms)"))
                                .binding(5000, () -> ConfigManager.ModConfig.server_chat_cooldown, newVal -> ConfigManager.ModConfig.server_chat_cooldown = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 10000).step(50).formatValue(v -> Text.literal(v + " ms"))).build())

                        .option(Option.<Double>createBuilder().name(Text.literal("Animation Duration (ms)"))
                                .binding(350.0, () -> ConfigManager.ModConfig.animation_duration, newVal -> ConfigManager.ModConfig.animation_duration = newVal)
                                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(50.0, 1000.0).step(10.0).formatValue(v -> Text.literal(v.intValue() + " ms"))).build())

                        // Text Format Codes
                        .group(OptionGroup.createBuilder().name(Text.literal("Chat Format Codes"))
                                .description(OptionDescription.of(Text.literal("Minecraft formatting codes (e.g. 'a' for green, 'c' for red)")))
                                .option(Option.<String>createBuilder().name(Text.literal("Mod Name Tag"))
                                        .binding("d", () -> ConfigManager.ModConfig.format_code_modname, newVal -> ConfigManager.ModConfig.format_code_modname = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Highlight (Names)"))
                                        .binding("b", () -> ConfigManager.ModConfig.format_code_highlight, newVal -> ConfigManager.ModConfig.format_code_highlight = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Info / Action"))
                                        .binding("e", () -> ConfigManager.ModConfig.format_code_info, newVal -> ConfigManager.ModConfig.format_code_info = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Sender Tag"))
                                        .binding("a", () -> ConfigManager.ModConfig.format_code_sender, newVal -> ConfigManager.ModConfig.format_code_sender = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Unhighlight 1 (Brackets)"))
                                        .binding("8", () -> ConfigManager.ModConfig.format_code_unhighlight1, newVal -> ConfigManager.ModConfig.format_code_unhighlight1 = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Unhighlight 2 (Body)"))
                                        .binding("7", () -> ConfigManager.ModConfig.format_code_unhighlight2, newVal -> ConfigManager.ModConfig.format_code_unhighlight2 = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Error Message"))
                                        .binding("c", () -> ConfigManager.ModConfig.format_code_error, newVal -> ConfigManager.ModConfig.format_code_error = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .option(Option.<String>createBuilder().name(Text.literal("Success Message"))
                                        .binding("a", () -> ConfigManager.ModConfig.format_code_success, newVal -> ConfigManager.ModConfig.format_code_success = newVal)
                                        .controller(StringControllerBuilder::create).build())
                                .build())
                        .build())

                .save(ConfigManager::saveConfig)
                .build()
                .generateScreen(parent);
    }
}