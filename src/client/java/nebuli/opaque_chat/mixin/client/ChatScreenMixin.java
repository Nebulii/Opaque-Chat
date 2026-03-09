package nebuli.opaque_chat.mixin.client;

import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.ContactData;
import nebuli.opaque_chat.gui.CreateGroupScreen;
import nebuli.opaque_chat.managers.ContactManager;
import nebuli.opaque_chat.managers.CryptoManager;
import nebuli.opaque_chat.managers.IdentityManager;
import nebuli.opaque_chat.managers.RequestManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static nebuli.opaque_chat.OpaqueChatClient.isOcOpen;
import static nebuli.opaque_chat.managers.ConfigManager.ModConfig.*;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected TextFieldWidget chatField;

    @Unique
    private TextFieldWidget opaqueChatField;
    @Unique
    private ButtonWidget toggleButton;
    @Unique
    private ButtonWidget requestsButton;
    @Unique
    private ButtonWidget createGroupButton;
    @Unique
    private ButtonWidget actionButton;
    @Unique
    private TextFieldWidget searchBox;
    @Unique
    private String selectedContact = null;
    @Unique
    private boolean showingRequests = false;
    @Unique
    private float slideProgress = 0.0f;
    @Unique
    private long lastRenderTime = 0;
    @Unique
    private int scrollOffset = 0;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private float easeInOutCubic(float x) {
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3f) / 2f;
    }

    @Unique
    private int getCurrentSplitX() {
        float easedProgress = easeInOutCubic(this.slideProgress);
        return (int) (this.width - (this.width / 2.0f) * easedProgress);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.lastRenderTime = Util.getMeasuringTimeMs();

        this.toggleButton = ButtonWidget.builder(Text.literal("OC"), button -> isOcOpen = !isOcOpen).dimensions(this.width - outer_gui_button_width - 2, this.height - outer_gui_button_height - 16, outer_gui_button_width, outer_gui_button_height).build();

        this.requestsButton = ButtonWidget.builder(Text.literal("Requests"), button -> {
            this.showingRequests = !this.showingRequests;
            this.selectedContact = null;
            button.setMessage(Text.literal(this.showingRequests ? "Requests" : "Contacts"));
        }).dimensions(this.width, 20, 80, 20).build();

        this.createGroupButton = ButtonWidget.builder(Text.literal("Create Group"), button -> {
            if (this.client != null) {
                this.client.setScreen(new CreateGroupScreen(this));
            }
        }).dimensions(this.width, 20, 80, 20).build();

        this.actionButton = ButtonWidget.builder(Text.literal("Action"), button -> {
            if (this.selectedContact != null && this.client != null && this.client.player != null) {
                String target = this.selectedContact;
                if (RequestManager.incomingRequests.contains(target.toLowerCase())) {
                    this.client.player.networkHandler.sendChatCommand("oc accept " + target);
                } else {
                    this.client.player.networkHandler.sendChatCommand("oc invite " + target);
                }
            }
        }).dimensions(this.width, inner_gui_button_y, inner_gui_button_width, inner_gui_button_height).build();
        this.actionButton.visible = false;

        this.opaqueChatField = new TextFieldWidget(this.textRenderer, this.width, this.height - 14, 0, chat_input_height, Text.literal("Secure Message"));
        this.opaqueChatField.setMaxLength(1280);
        this.opaqueChatField.setDrawsBackground(false);

        this.searchBox = new TextFieldWidget(this.textRenderer, this.width, 20, contact_column_width - 12, search_input_height, Text.literal("Search"));
        this.searchBox.setDrawsBackground(true);

        this.addDrawableChild(this.opaqueChatField);
        this.addDrawableChild(this.searchBox);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (isOcOpen && this.opaqueChatField.isFocused() && this.opaqueChatField.isVisible()) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                String message = this.opaqueChatField.getText().trim();

                if (!message.isEmpty() && this.selectedContact != null) {
                    try {
                        if (this.selectedContact.startsWith("#")) {
                            String groupName = this.selectedContact.substring(1);
                            nebuli.opaque_chat.data.GroupData targetGroup = nebuli.opaque_chat.managers.GroupManager.groups.values().stream().filter(g -> g.name.equals(groupName)).findFirst().orElse(null);

                            if (targetGroup != null && this.client != null && this.client.player != null) {
                                byte[] groupAesKey = java.util.Base64.getDecoder().decode(targetGroup.aes_key_base64);
                                List<String> messageChunks = CryptoManager.chunkMessageAtSpaces(message, 120);

                                for (String chunk : messageChunks) {
                                    String encryptedText = CryptoManager.encryptMessage(chunk, groupAesKey);
                                    nebuli.opaque_chat.managers.MessageQueueManager.enqueueMessage("!oc_gmsg " + targetGroup.uuid + " " + targetGroup.version + " " + encryptedText);
                                }

                                nebuli.opaque_chat.managers.GroupManager.groupSessionHistory.computeIfAbsent(targetGroup.uuid, k -> new ArrayList<>()).add("§" + format_code_unhighlight1 + "[You]: §" + format_code_unhighlight2 + message);
                            }
                        } else {
                            ContactData targetContact = ContactManager.contacts.get(this.selectedContact.toLowerCase());
                            if (targetContact != null && IdentityManager.currentIdentity != null) {
                                byte[] sharedSecret = CryptoManager.getSharedSecret(IdentityManager.currentIdentity.private_key, targetContact.public_key);
                                List<String> messageChunks = CryptoManager.chunkMessageAtSpaces(message, 120);

                                if (this.client != null && this.client.player != null) {
                                    for (String chunk : messageChunks) {
                                        String encryptedText = CryptoManager.encryptMessage(chunk, sharedSecret);
                                        nebuli.opaque_chat.managers.MessageQueueManager.enqueueMessage("!oc_msg " + this.selectedContact + " " + targetContact.version + " " + encryptedText);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to encrypt/send message", e);
                    }
                    this.opaqueChatField.setText("");
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void splitBackgroundBar(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int currentSplit = getCurrentSplitX();

        if (slideProgress == 0.0f) {
            context.fill(x1, y1, x2, y2, color);
        } else {
            context.fill(x1, y1, currentSplit - 1, y2, color);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        long currentTime = Util.getMeasuringTimeMs();
        float dt = (currentTime - lastRenderTime);
        this.lastRenderTime = currentTime;

        if (isOcOpen && this.slideProgress < 1.0) {
            this.slideProgress = (float) Math.min(1.0, this.slideProgress + (dt / animation_duration));
        } else if (!isOcOpen && this.slideProgress > 0.0f) {
            this.slideProgress = (float) Math.max(0.0, this.slideProgress - (dt / animation_duration));
        }

        int currentSplit = getCurrentSplitX();
        this.chatField.setWidth(currentSplit - 8);

        this.toggleButton.setX(currentSplit - outer_gui_button_width);
        this.toggleButton.render(context, mouseX, mouseY, delta);

        if (this.slideProgress > 0.0f) {
            int panelX = currentSplit + 1;
            int panelWidth = (this.width - 3) - currentSplit;
            int panelY = 2;

            int panelHeight = this.height - panel_distance_from_top - 4;

            int chatColX = panelX + contact_column_width;
            int chatColWidth = panelWidth - contact_column_width;

            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, contacts_bg);
            context.fill(chatColX, panelY, panelX + panelWidth, panelY + panelHeight, chat_area_bg);

            context.fill(chatColX, panelY, chatColX + 1, panelY + panelHeight, outline_color);
            context.fill(chatColX, this.height - chat_input_height - 6, panelX + panelWidth, this.height - chat_input_height - 5, outline_color);

            this.searchBox.setX(panelX + 4);
            this.searchBox.setY(panelY + 4);
            this.searchBox.visible = true;

            int btnWidth = (contact_column_width - 12) / 2;
            int bottomBtnY = this.height - 26;

            this.requestsButton.setX(panelX + 4);
            this.requestsButton.setY(bottomBtnY);
            this.requestsButton.setWidth(btnWidth);
            this.requestsButton.visible = true;
            this.requestsButton.render(context, mouseX, mouseY, delta);

            this.createGroupButton.setX(panelX + 8 + btnWidth);
            this.createGroupButton.setY(bottomBtnY);
            this.createGroupButton.setWidth(btnWidth);
            this.createGroupButton.visible = true;
            this.createGroupButton.render(context, mouseX, mouseY, delta);

            renderContactList(context, panelX, mouseX, mouseY, panelY + search_input_height + 12);

            if (selectedContact == null) {
                this.opaqueChatField.visible = false;
                this.actionButton.visible = false;
                String prompt = "Select a contact to chat";
                int promptX = chatColX + (chatColWidth / 2) - (this.textRenderer.getWidth(prompt) / 2);
                context.drawTextWithShadow(this.textRenderer, prompt, promptX, panelHeight / 2, prompt_color);
            } else {
                boolean isGroup = selectedContact.startsWith("#");
                boolean isSavedContact = !isGroup && ContactManager.contacts.containsKey(selectedContact.toLowerCase());

                if (isGroup) {
                    String groupName = selectedContact.substring(1);
                    isGroup = nebuli.opaque_chat.managers.GroupManager.groups.values().stream().anyMatch(g -> g.name.equals(groupName));
                }

                if (isSavedContact || isGroup) {
                    this.opaqueChatField.visible = true;
                    this.opaqueChatField.setX(chatColX + 4);
                    this.opaqueChatField.setY(this.height - chat_input_height - 2);
                    this.opaqueChatField.setWidth(chatColWidth - 8);
                    this.opaqueChatField.setHeight(chat_input_height);
                    this.actionButton.visible = false;

                    List<String> history;
                    if (selectedContact.startsWith("#")) {
                        String groupName = selectedContact.substring(1);
                        String uuid = nebuli.opaque_chat.managers.GroupManager.groups.values().stream().filter(g -> g.name.equals(groupName)).map(g -> g.uuid).findFirst().orElse("");
                        history = nebuli.opaque_chat.managers.GroupManager.groupSessionHistory.get(uuid);
                    } else {
                        history = ContactManager.sessionHistory.get(selectedContact.toLowerCase());
                    }
                    if (history != null && !history.isEmpty()) {
                        int topBound = panelY + panel_distance_from_top + 20;
                        int bottomBound = this.height - chat_input_height - 8;

                        List<net.minecraft.text.OrderedText> allWrappedLines = new java.util.ArrayList<>();
                        for (String msg : history) {
                            allWrappedLines.addAll(this.textRenderer.wrapLines(Text.literal(msg), chatColWidth - 12));
                        }

                        int lineHeight = this.textRenderer.fontHeight + 2;
                        int totalHeight = allWrappedLines.size() * lineHeight;
                        int visibleHeight = bottomBound - topBound;

                        int maxScroll = Math.max(0, totalHeight - visibleHeight);
                        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
                        if (this.scrollOffset < 0) this.scrollOffset = 0;

                        int currentY = bottomBound + this.scrollOffset;

                        for (int i = allWrappedLines.size() - 1; i >= 0; i--) {
                            currentY -= lineHeight;

                            if (currentY >= topBound && currentY <= bottomBound - lineHeight) {
                                context.drawTextWithShadow(this.textRenderer, allWrappedLines.get(i), chatColX + 6, currentY, text_primary);
                            }
                        }
                    }
                } else {
                    this.opaqueChatField.visible = false;
                    this.actionButton.visible = true;

                    this.actionButton.setX(chatColX + (chatColWidth / 2) - (inner_gui_button_width / 2));
                    this.actionButton.setY(panelY + (panelHeight / 2) - (inner_gui_button_height / 2));

                    if (RequestManager.incomingRequests.contains(selectedContact.toLowerCase())) {
                        this.actionButton.setMessage(Text.literal("Accept Invite"));
                    } else {
                        this.actionButton.setMessage(Text.literal("Send Invite"));
                    }

                    this.actionButton.render(context, mouseX, mouseY, delta);
                }
            }
        } else {
            this.searchBox.visible = false;
            this.opaqueChatField.visible = false;
            this.requestsButton.visible = false;
            this.createGroupButton.visible = false;
            this.actionButton.visible = false;
        }
    }

    @Unique
    private List<String> getDisplayEntries() {
        String searchText = this.searchBox.getText().toLowerCase();
        List<String> entries = new ArrayList<>();

        for (String groupName : nebuli.opaque_chat.managers.GroupManager.groups.values().stream().map(g -> g.name).toList()) {
            if (groupName.toLowerCase().contains(searchText)) entries.add("#" + groupName);
        }

        if (this.showingRequests) {
            for (String req : RequestManager.incomingRequests) {
                if (req.toLowerCase().contains(searchText)) entries.add(req);
            }
        } else {
            for (String name : ContactManager.contacts.values().stream().map(c -> c.username).toList()) {
                if (name.toLowerCase().contains(searchText)) entries.add(name);
            }

            if (this.client != null && this.client.getNetworkHandler() != null) {
                String myName = this.client.getSession().getUsername();
                for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
                    String name = entry.getProfile().name();
                    if (!name.equalsIgnoreCase(myName) && !ContactManager.contacts.containsKey(name.toLowerCase()) && name.toLowerCase().contains(searchText) && !entries.contains(name)) {

                        entries.add(name);
                    }
                }
            }
        }
        return entries;
    }

    @Unique
    private void renderContactList(DrawContext context, int panelX, int mouseX, int mouseY, int startY) {
        int listX = panelX + 4;
        List<String> entries = getDisplayEntries();

        if (this.showingRequests && entries.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "§" + format_code_unhighlight2 + "No pending requests", listX + 4, startY, contact_offline_icon);
            return;
        }

        for (int i = 0; i < entries.size(); i++) {
            String name = entries.get(i);
            int entryY = startY + (i * contact_entry_height);

            boolean isContact = ContactManager.contacts.containsKey(name.toLowerCase());
            boolean isSelected = name.equals(selectedContact);
            boolean isHovered = mouseX >= listX && mouseX <= listX + contact_column_width - 8 && mouseY >= entryY && mouseY < entryY + contact_entry_height;

            if (isSelected) {
                context.fill(listX, entryY - 2, listX + contact_column_width - 8, entryY + contact_entry_height - 2, contact_selected_bg);
            } else if (isHovered) {
                context.fill(listX, entryY - 2, listX + contact_column_width - 8, entryY + contact_entry_height - 2, contact_hover_bg);
            }

            context.drawTextWithShadow(this.textRenderer, isContact ? "●" : "○", listX + 2, entryY, isContact ? contact_online_icon : contact_offline_icon);
            int textOffsetX = listX + 10;

            int textColor = isContact ? (isSelected ? contact_online_icon : (isHovered ? text_hover : text_primary)) : contact_offline_icon;
            context.drawTextWithShadow(this.textRenderer, name, textOffsetX, entryY, textColor);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onContactClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        if (button != 0) return;

        if (this.toggleButton.isMouseOver(mouseX, mouseY)) {
            this.toggleButton.mouseClicked(click, doubled);
            cir.setReturnValue(true);
            return;
        }

        if (!isOcOpen || this.slideProgress < 1.0f) return;

        if (this.requestsButton.visible && this.requestsButton.isMouseOver(mouseX, mouseY)) {
            this.requestsButton.mouseClicked(click, doubled);
            cir.setReturnValue(true);
            return;
        }
        if (this.createGroupButton.visible && this.createGroupButton.isMouseOver(mouseX, mouseY)) {
            this.createGroupButton.mouseClicked(click, doubled);
            cir.setReturnValue(true);
            return;
        }
        if (this.actionButton.visible && this.actionButton.isMouseOver(mouseX, mouseY)) {
            this.actionButton.mouseClicked(click, doubled);
            cir.setReturnValue(true);
            return;
        }

        if (this.searchBox.isMouseOver(mouseX, mouseY) || this.opaqueChatField.isMouseOver(mouseX, mouseY)) {
            return;
        }

        int currentSplit = getCurrentSplitX();
        int panelX = currentSplit + 1;
        int listX = panelX + 4;
        int listY = 30;

        List<String> entries = getDisplayEntries();

        for (int i = 0; i < entries.size(); i++) {
            int entryY = listY + (i * contact_entry_height);

            if (mouseX >= listX && mouseX <= listX + contact_column_width - 8 && mouseY >= entryY && mouseY < entryY + contact_entry_height) {

                this.selectedContact = entries.get(i);

                if (this.client != null) {
                    this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }

                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (isOcOpen && this.slideProgress == 1.0f && this.selectedContact != null) {
            int currentSplit = getCurrentSplitX();
            int chatColX = currentSplit + 1 + contact_column_width;

            if (mouseX >= chatColX && mouseX <= this.width && mouseY >= 2 && mouseY <= this.height - 18) {

                this.scrollOffset -= (int) (verticalAmount * 12);

                cir.setReturnValue(true);
            }
        }
    }
}