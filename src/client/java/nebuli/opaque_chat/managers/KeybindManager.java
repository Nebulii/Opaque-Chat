package nebuli.opaque_chat.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {
    private static KeyBinding openGuiKey;
    private static final KeyBinding.Category OpaqueChatCategory = KeyBinding.Category.create(Identifier.of("category.opaque_chat.keys"));

    public static void register() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.opaque_chat.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Y,
                OpaqueChatCategory
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    //client.setScreen(new OpaqueChatScreen());
                }
            }
        });
    }
}