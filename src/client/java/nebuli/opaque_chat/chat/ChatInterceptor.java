package nebuli.opaque_chat.chat;

import nebuli.opaque_chat.*;
import nebuli.opaque_chat.data.ContactData;
import nebuli.opaque_chat.managers.ContactManager;
import nebuli.opaque_chat.managers.CryptoManager;
import nebuli.opaque_chat.managers.IdentityManager;
import nebuli.opaque_chat.managers.RequestManager;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;

import static nebuli.opaque_chat.ConfigManager.ModConfig.*;

public class ChatInterceptor {
    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String rawText = message.getString();
            int ocIndex = rawText.indexOf("!oc_");

            if (ocIndex != -1) {
                if (sender == null) return true;

                String myName = MinecraftClient.getInstance().getSession().getUsername();

                String senderName = sender.name();
                if (senderName.equals(myName)) {
                    return false;
                }

                String payload = rawText.substring(ocIndex);
                String[] parts = payload.split(" ", 3);
                if (parts.length >= 2) {
                    String command = parts[0];
                    String targetPlayer = parts[1];

                    if (!targetPlayer.equalsIgnoreCase(myName)) {
                        return false;
                    }

                    if (command.equals("!oc_req")) {
                        RequestManager.addRequest(senderName);

                        MutableText notification = Text.literal("§" + format_code_modname + "[Opaque Chat] §" + format_code_highlight + senderName + " §rwants to chat securely. ")
                                .append(Text.literal("§" + format_code_info + "[ACCEPT]")
                                        .styled(style -> style
                                                .withClickEvent(new ClickEvent.RunCommand("/oc accept " + senderName))
                                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to accept secure chat")))
                                        ));

                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(notification);
                    }
                    else if (command.equals("!oc_key") && parts.length == 3) {
                        try {
                            String senderUuid = sender.id().toString();
                            String publicKey = parts[2].trim();

                            boolean alreadyConnected = ContactManager.contacts != null &&
                                    ContactManager.contacts.containsKey(senderName.toLowerCase());

                            ContactData newContact = new ContactData(senderName, senderUuid, senderUuid, publicKey);
                            ContactManager.addContact(newContact);

                            if (!alreadyConnected) {
                                sendPublicKey(senderName);
                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                        Text.literal("§" + format_code_modname + "[Opaque Chat]§r Secure link established with §" + format_code_highlight + senderName)
                                );
                            }
                            RequestManager.removeRequest(senderName);
                        } catch (Exception e) {
                            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to parse EC key", e);
                        }
                    }
                    else if (command.equals("!oc_msg") && parts.length == 3) {
                        try {
                            ContactData senderContact = ContactManager.contacts.get(senderName.toLowerCase());
                            if (senderContact != null && IdentityManager.currentIdentity != null) {
                                String encryptedPayload = parts[2].trim();
                                byte[] sharedSecret = CryptoManager.getSharedSecret(IdentityManager.currentIdentity.private_key, senderContact.public_key);
                                String decryptedText = CryptoManager.decryptMessage(encryptedPayload, sharedSecret);

                                ContactManager.sessionHistory.computeIfAbsent(senderName.toLowerCase(), k -> new ArrayList<>())
                                        .add("§" + format_code_sender + "[" + senderName + "]: §r" + decryptedText);

                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                        Text.literal("§" + format_code_modname + "[Opaque Chat] §7New secure message from §" + format_code_highlight + senderName)
                                );
                            }
                        } catch (Exception e) {
                            OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to decrypt incoming message", e);
                        }
                    }
                }

                return false;
            }

            return true;
        });
    }

    public static void sendPublicKey(String targetPlayer) {
        if (IdentityManager.currentIdentity == null) return;

        String payload = "!oc_key " + targetPlayer + " " + IdentityManager.currentIdentity.public_key;

        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(payload);
        }
    }
}