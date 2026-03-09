package nebuli.opaque_chat.gui;

import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.ContactData;
import nebuli.opaque_chat.data.GroupData;
import nebuli.opaque_chat.managers.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static nebuli.opaque_chat.managers.ConfigManager.ModConfig.*;

public class CreateGroupScreen extends Screen {
    private final Screen parent;
    private final List<String> allContacts;
    private final List<String> selectedContacts = new ArrayList<>();
    private final List<ButtonWidget> contactButtons = new ArrayList<>();
    private TextFieldWidget nameField;
    private TextFieldWidget searchField;
    private ButtonWidget createButton;
    private List<String> filteredContacts;
    private int currentPage = 0;
    private int contactsPerPage;

    public CreateGroupScreen(Screen parent) {
        super(Text.literal("Create Secure Group"));
        this.parent = parent;
        this.allContacts = new ArrayList<>(ContactManager.contacts.keySet());
        this.filteredContacts = new ArrayList<>(this.allContacts);
    }

    @Override
    protected void init() {
        super.init();

        int columns = Math.max(1, ConfigManager.ModConfig.group_creation_columns);
        this.contactsPerPage = columns * 7;

        this.nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 35, 200, 20, Text.literal("Group Name"));
        this.nameField.setPlaceholder(Text.literal("Enter Group Name..."));
        this.nameField.setMaxLength(32);
        this.addDrawableChild(this.nameField);
        this.setInitialFocus(this.nameField);

        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 65, 200, 16, Text.literal("Search"));
        this.searchField.setPlaceholder(Text.literal("Search contacts..."));
        this.searchField.setChangedListener(text -> {
            this.filteredContacts = this.allContacts.stream()
                    .filter(c -> c.toLowerCase().contains(text.toLowerCase()))
                    .toList();
            this.currentPage = 0;
            this.refreshContactButtons();
        });
        this.addDrawableChild(this.searchField);

        ButtonWidget prevButton = ButtonWidget.builder(Text.literal("<"), button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.refreshContactButtons();
            }
        }).dimensions(this.width / 2 - 125, 220, 20, 20).build();

        ButtonWidget nextButton = ButtonWidget.builder(Text.literal(">"), button -> {
            if ((this.currentPage + 1) * contactsPerPage < this.filteredContacts.size()) {
                this.currentPage++;
                this.refreshContactButtons();
            }
        }).dimensions(this.width / 2 + 105, 220, 20, 20).build();

        this.addDrawableChild(prevButton);
        this.addDrawableChild(nextButton);

        this.createButton = ButtonWidget.builder(Text.literal("Create"), button -> executeCreateGroup())
                .dimensions(this.width / 2 - 105, this.height - 35, 100, 20).build();

        ButtonWidget cancelButton = ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(this.width / 2 + 5, this.height - 35, 100, 20).build();

        this.addDrawableChild(this.createButton);
        this.addDrawableChild(cancelButton);

        this.refreshContactButtons();
    }

    private void refreshContactButtons() {
        for (ButtonWidget btn : this.contactButtons) {
            this.remove(btn);
        }
        this.contactButtons.clear();

        int startIdx = this.currentPage * contactsPerPage;
        int endIdx = Math.min(startIdx + contactsPerPage, this.filteredContacts.size());

        int columns = Math.max(1, ConfigManager.ModConfig.group_creation_columns);
        int buttonWidth = 95;
        int spacing = 10;

        int totalGridWidth = (columns * buttonWidth) + ((columns - 1) * spacing);
        int startX = (this.width / 2) - (totalGridWidth / 2);
        int startY = 92;

        for (int i = startIdx; i < endIdx; i++) {
            String contactName = this.filteredContacts.get(i);
            int localIdx = i - startIdx;

            int col = localIdx % columns;
            int row = localIdx / columns;

            int btnX = startX + (col * (buttonWidth + spacing));
            int btnY = startY + (row * 24);

            ButtonWidget toggleBtn = ButtonWidget.builder(getButtonText(contactName), button -> {
                if (this.selectedContacts.contains(contactName)) {
                    this.selectedContacts.remove(contactName);
                } else {
                    this.selectedContacts.add(contactName);
                }
                button.setMessage(getButtonText(contactName));
            }).dimensions(btnX, btnY, buttonWidth, 20).build();

            this.contactButtons.add(toggleBtn);
            this.addDrawableChild(toggleBtn);
        }
    }

    private Text getButtonText(String name) {
        if (this.selectedContacts.contains(name)) {
            return Text.literal("§" + format_code_success + "[x] " + name);
        }
        return Text.literal("§" + format_code_unhighlight2 + "[ ] " + name);
    }

    private void executeCreateGroup() {
        String groupName = this.nameField.getText().trim();

        if (groupName.isEmpty() || this.selectedContacts.isEmpty() || this.client == null || this.client.player == null) {
            return;
        }

        List<String> groupRoster = new ArrayList<>(this.selectedContacts);
        groupRoster.add(this.client.getSession().getUsername());

        GroupData newGroup = GroupManager.createGroup(groupName, groupRoster);

        try {
            String myPrivateKey = IdentityManager.currentIdentity.private_key;

            for (String member : this.selectedContacts) {
                ContactData targetContact = ContactManager.contacts.get(member);
                if (targetContact == null) continue;

                byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, targetContact.public_key);
                String encryptedGroupKey = CryptoManager.encryptMessage(newGroup.aes_key_base64, sharedSecret);

                MessageQueueManager.enqueueMessage("!oc_ginv " + member + " " + newGroup.uuid + " " + encryptedGroupKey + " " + groupName);
            }

            this.client.player.sendMessage(Text.literal("§" + format_code_success + "[Opaque Chat] Group '" + groupName + "' created! Invites queued."), false);
        } catch (Exception e) {
            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to encrypt group invites!", e);
            this.client.player.sendMessage(Text.literal("§" + format_code_error + "[Opaque Chat] Critical error creating group!"), false);
        }

        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Select Members (" + this.selectedContacts.size() + " selected):", this.width / 2 - 100, 82, 0xAAAAAA);

        this.createButton.active = !this.nameField.getText().trim().isEmpty() && !this.selectedContacts.isEmpty();

        int maxPages = Math.max(1, (int) Math.ceil((double) this.filteredContacts.size() / contactsPerPage));
        context.drawCenteredTextWithShadow(this.textRenderer, "Page " + (this.currentPage + 1) + " of " + maxPages, this.width / 2, 226, 0xAAAAAA);

        super.render(context, mouseX, mouseY, delta);

        if (this.nameField.getText().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "Enter Group Name...", this.nameField.getX() + 4, this.nameField.getY() + 6, 0x888888);
        }
        if (this.searchField.getText().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "Search contacts...", this.searchField.getX() + 4, this.searchField.getY() + 4, 0x888888);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.createButton.active && !this.searchField.isFocused()) {
                executeCreateGroup();
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}