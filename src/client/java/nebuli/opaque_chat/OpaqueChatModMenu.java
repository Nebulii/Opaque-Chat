package nebuli.opaque_chat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import nebuli.opaque_chat.gui.YaclScreenBuilder;

public class OpaqueChatModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return YaclScreenBuilder::build;
    }
}