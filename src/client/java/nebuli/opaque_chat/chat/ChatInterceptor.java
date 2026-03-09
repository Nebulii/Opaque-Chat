package nebuli.opaque_chat.chat;

import nebuli.opaque_chat.OpaqueChatClient;
import nebuli.opaque_chat.data.ContactData;
import nebuli.opaque_chat.data.GroupData;
import nebuli.opaque_chat.managers.*;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;

import static nebuli.opaque_chat.managers.ConfigManager.ModConfig.*;

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


                    if (command.equals("!oc_req")) {
                        String targetPlayer = parts[1];
                        if (!targetPlayer.equalsIgnoreCase(myName)) {
                            return false;
                        }

                        RequestManager.addRequest(senderName);

                        MutableText notification = Text.literal("§" + format_code_modname + "[Opaque Chat] §" + format_code_highlight + senderName + "§r wants to chat securely. ")
                                .append(Text.literal("§" + format_code_info + "[ACCEPT]")
                                        .styled(style -> style
                                                .withClickEvent(new ClickEvent.RunCommand("/oc accept " + senderName))
                                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to accept secure chat")))
                                        ));

                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(notification);
                    } else // !oc_gmsg <GroupUUID> <KeyVersion> <EncryptedText>
                        if (command.equals("!oc_gmsg") && parts.length == 3) {
                            String[] msgData = parts[2].split(" ", 2);

                            if (msgData.length == 2) {
                                String groupUuid = parts[1];
                                int messageKeyVersion = Integer.parseInt(msgData[0]);
                                String encryptedPayload = msgData[1].trim();

                                if (GroupManager.groups.containsKey(groupUuid)) {
                                    GroupData group = GroupManager.groups.get(groupUuid);

                                    if (messageKeyVersion > group.version) {
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_error + "[Opaque Chat] Key mismatch in '" + group.name + "'! " + senderName + " is using Key v" + messageKeyVersion + " but you only have v" + group.version + ". You may have been kicked.")
                                        );
                                        return false;
                                    }

                                    if (messageKeyVersion < group.version) {
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_error + "[Opaque Chat] " + senderName + " tried to send a message in '" + group.name + "' using an outdated key (v" + messageKeyVersion + ").")
                                        );
                                        return false;
                                    }

                                    try {
                                        byte[] groupAesKey = java.util.Base64.getDecoder().decode(group.aes_key_base64);
                                        String decryptedText = CryptoManager.decryptMessage(encryptedPayload, groupAesKey);

                                        nebuli.opaque_chat.managers.GroupManager.groupSessionHistory.computeIfAbsent(groupUuid, k -> new ArrayList<>())
                                                .add("§" + format_code_sender + "[" + senderName + "]: §r" + decryptedText);

                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_modname + "[Opaque Chat] §7New message in group §" + format_code_highlight + group.name)
                                        );
                                    } catch (Exception e) {
                                        OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to decrypt group message despite matching version", e);
                                    }
                                }
                            }
                            return false;
                        } else if (command.equals("!oc_ginv") && parts.length == 3) {
                            String[] inviteData = parts[2].split(" ", 3);
                            if (inviteData.length == 3) {
                                String groupUuid = inviteData[0];
                                String encryptedKey = inviteData[1];
                                String groupName = inviteData[2];

                                try {
                                    ContactData senderContact = ContactManager.contacts.get(senderName.toLowerCase());
                                    if (senderContact != null && IdentityManager.currentIdentity != null) {
                                        byte[] sharedSecret = CryptoManager.getSharedSecret(IdentityManager.currentIdentity.private_key, senderContact.public_key);
                                        String decryptedKeyBase64 = CryptoManager.decryptMessage(encryptedKey, sharedSecret);

                                        GroupData newGroup = new GroupData(groupUuid, groupName, decryptedKeyBase64, new ArrayList<>(), 1);
                                        GroupManager.addGroup(newGroup);

                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_modname + "[Opaque Chat] §rAdded to group §" + format_code_highlight + groupName + " §rby " + senderName)
                                        );
                                    }
                                } catch (Exception e) {
                                    OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to process group invite", e);
                                }
                            }
                        } else if (command.equals("!oc_key") && parts.length == 3) {
                            try {
                                String senderUuid = sender.id().toString();
                                String[] keyData = parts[2].split(" ", 2);

                                int keyVersion = 1;
                                String publicKey = parts[2].trim();

                                if (keyData.length == 2) {
                                    try {
                                        keyVersion = Integer.parseInt(keyData[0]);
                                        publicKey = keyData[1].trim();
                                    } catch (NumberFormatException ignored) {
                                    }
                                }

                                boolean alreadyConnected = ContactManager.contacts != null &&
                                        ContactManager.contacts.containsKey(senderName.toLowerCase());

                                ContactData newContact = new ContactData(senderName, senderUuid, senderUuid, publicKey, keyVersion);
                                ContactManager.addContact(newContact);

                                if (!alreadyConnected) {
                                    sendPublicKey(senderName);
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                            Text.literal("§" + format_code_modname + "[Opaque Chat]§r Secure link established with §" + format_code_highlight + senderName)
                                    );
                                } else {
                                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                            Text.literal("§" + format_code_modname + "[Opaque Chat]§7 Automatically updated key (v" + keyVersion + ") for " + senderName)
                                    );
                                }
                                RequestManager.removeRequest(senderName);
                            } catch (Exception e) {
                                OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to parse EC key", e);
                            }
                        } else if (command.equals("!oc_msg") && parts.length == 3) {
                            try {
                                String[] msgData = parts[2].split(" ", 2);
                                if (msgData.length == 2) {
                                    int usedKeyVersion = Integer.parseInt(msgData[0]);
                                    String encryptedPayload = msgData[1].trim();

                                    if (IdentityManager.currentIdentity != null && usedKeyVersion < IdentityManager.currentIdentity.version) {
                                        sendPublicKey(senderName); // Kick back our new public key quietly
                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_error + "[Opaque Chat] " + senderName + " used an old key. Auto-healing...")
                                        );
                                        return false;
                                    }

                                    ContactData senderContact = ContactManager.contacts.get(senderName.toLowerCase());
                                    if (senderContact != null && IdentityManager.currentIdentity != null) {
                                        byte[] sharedSecret = CryptoManager.getSharedSecret(IdentityManager.currentIdentity.private_key, senderContact.public_key);
                                        String decryptedText = CryptoManager.decryptMessage(encryptedPayload, sharedSecret);

                                        ContactManager.sessionHistory.computeIfAbsent(senderName.toLowerCase(), k -> new ArrayList<>())
                                                .add("§" + format_code_sender + "[" + senderName + "]: §r" + decryptedText);

                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_modname + "[Opaque Chat] §7New secure message from §" + format_code_highlight + senderName)
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to decrypt incoming message", e);
                            }
                        } else if (command.equals("!oc_gupd") && parts.length == 3) {
                            // Fix: Split by 3 to separate the UUID, Version, and AES Key
                            String[] updateData = parts[2].split(" ", 3);
                            if (updateData.length == 3) {
                                String groupUuid = updateData[0];
                                int newVersion = Integer.parseInt(updateData[1]);
                                String encryptedKey = updateData[2].trim();

                                try {
                                    ContactData senderContact = ContactManager.contacts.get(senderName.toLowerCase());
                                    if (senderContact != null && GroupManager.groups.containsKey(groupUuid)) {

                                        byte[] sharedSecret = CryptoManager.getSharedSecret(IdentityManager.currentIdentity.private_key, senderContact.public_key);
                                        String newKeyBase64 = CryptoManager.decryptMessage(encryptedKey, sharedSecret);

                                        nebuli.opaque_chat.data.GroupData group = GroupManager.groups.get(groupUuid);
                                        group.aes_key_base64 = newKeyBase64;
                                        group.version = newVersion; // Save the new incremented version!
                                        GroupManager.saveGroups();

                                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                                Text.literal("§" + format_code_modname + "[Opaque Chat] §7AES Key rotated (v" + newVersion + ") for group §" + format_code_highlight + group.name)
                                        );
                                    }
                                } catch (Exception e) {
                                    OpaqueChatClient.LOGGER.error("[Opaque Chat] Failed to process key rotation", e);
                                }
                            }
                        } else if (command.equals("!oc_gkicked") && parts.length == 3) {
                            String groupUuid = parts[2].trim();

                            if (GroupManager.groups.containsKey(groupUuid)) {
                                String groupName = GroupManager.groups.get(groupUuid).name;

                                GroupManager.removeGroup(groupUuid);

                                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                                        Text.literal("§" + format_code_error + "[Opaque Chat] You were kicked from group '" + groupName + "' by " + senderName)
                                );
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

        // !oc_key <Target> <Version> <PublicKey>
        String payload = "!oc_key " + targetPlayer + " " + IdentityManager.currentIdentity.version + " " + IdentityManager.currentIdentity.public_key;

        if (MinecraftClient.getInstance().player != null) {
            MessageQueueManager.enqueueMessage(payload);
        }
    }
}