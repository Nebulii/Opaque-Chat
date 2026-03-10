package nebuli.opaque_chat.managers;

import com.mojang.brigadier.arguments.StringArgumentType;
import nebuli.opaque_chat.chat.ChatInterceptor;
import nebuli.opaque_chat.data.ContactData;
import nebuli.opaque_chat.gui.YaclScreenBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static nebuli.opaque_chat.managers.ConfigManager.ModConfig.*;
import static nebuli.opaque_chat.managers.MessageQueueManager.getCurrentServerId;

public class CommandManager {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // This defines the base command: /oc
            dispatcher.register(ClientCommandManager.literal("oc")

                    // Subcommand: /oc help
                    .then(ClientCommandManager.literal("help").executes(context -> {
                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Available commands:"));

                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc help §8- Show this menu"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc config §8- Open the visual configuration screen"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc delay <ms> §8- Set the chat cooldown delay for the current server"));

                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc invite <target> §8- Start a secure handshake with a player"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc accept/reject <target> §8- Accept or reject a handshake"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc msg <target> <message> §8- Send a secure 1-on-1 message"));

                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc block/unblock <target> §8- Block a player from sending you packets"));

                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc identity reset §8- Delete and regenerate your ECC keys"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc contact add/remove <target> §8- Manage contacts"));

                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc group create <name> <members...> §8- Create a group"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc group <name> accept/reject/leave §8- Manage your group membership"));
                        context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc group <name> invite/kick <target> §8- (Owner Only) Manage members"));

                        return 1;
                    }))

                    // Subcommand: /oc invite <target>
                    .then(ClientCommandManager.literal("invite").then(ClientCommandManager.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                            return CommandSource.suggestMatching(MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream().map(entry -> entry.getProfile().name()), builder);
                        }
                        return builder.buildFuture();
                    }).executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "target");

                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Initiating silent handshake with §" + format_code_highlight + targetPlayer + "§r..."));

                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            RequestManager.outgoingRequests.add(targetPlayer.toLowerCase());

                            MessageQueueManager.enqueueMessage("!oc_req " + targetPlayer);
                        }

                        return 1;
                    })))

                    // Subcommand: /oc accept <target>
                    .then(ClientCommandManager.literal("accept").then(ClientCommandManager.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                            return CommandSource.suggestMatching(MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream().map(entry -> entry.getProfile().name()), builder);
                        }
                        return builder.buildFuture();
                    }).executes(context -> {
                        String target = StringArgumentType.getString(context, "target");
                        RequestManager.removeRequest(target);
                        RequestManager.outgoingRequests.add(target.toLowerCase());
                        ChatInterceptor.sendPublicKey(target);

                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Handshake accepted for §" + format_code_highlight + target));
                        return 1;
                    })))

                    // Subcommand: /oc reject <target>
                    .then(ClientCommandManager.literal("reject").then(ClientCommandManager.argument("target", StringArgumentType.word()).executes(context -> {
                        String target = StringArgumentType.getString(context, "target");
                        RequestManager.removeRequest(target);
                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Rejected handshake from §" + format_code_highlight + target));
                        return 1;
                    })))

                    // Subcommand: /oc block <target>
                    .then(ClientCommandManager.literal("block").then(ClientCommandManager.argument("target", StringArgumentType.word()).executes(context -> {
                        String target = StringArgumentType.getString(context, "target").toLowerCase();
                        if (!ConfigManager.ModConfig.blocked_players.contains(target)) {
                            ConfigManager.ModConfig.blocked_players.add(target);
                            ConfigManager.saveConfig();

                            ContactManager.contacts.remove(target);
                            ContactManager.saveContacts();

                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Blocked " + target + ". Their packets will now be dropped."));
                        }
                        return 1;
                    })))

                    // Subcommand: /oc unblock <target>
                    .then(ClientCommandManager.literal("unblock").then(ClientCommandManager.argument("target", StringArgumentType.word()).suggests((context, builder) -> CommandSource.suggestMatching(ConfigManager.ModConfig.blocked_players, builder)).executes(context -> {
                        String target = StringArgumentType.getString(context, "target").toLowerCase();
                        if (ConfigManager.ModConfig.blocked_players.contains(target)) {
                            ConfigManager.ModConfig.blocked_players.remove(target);
                            ConfigManager.saveConfig();
                            context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Unblocked " + target + "."));
                        }
                        return 1;
                    })))

                    // Subcommand: /oc msg <player> <message>
                    .then(ClientCommandManager.literal("msg").then(ClientCommandManager.argument("target", StringArgumentType.word()).then(ClientCommandManager.argument("text", StringArgumentType.greedyString()).executes(context -> {
                        String targetPlayer = StringArgumentType.getString(context, "target");
                        String text = StringArgumentType.getString(context, "text");

                        try {
                            String myPrivateKey = IdentityManager.currentIdentity.private_key;
                            ContactData targetContact = ContactManager.contacts.get(targetPlayer.toLowerCase());

                            if (targetContact == null) {
                                context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat] You don't have a secure link with §" + format_code_highlight + targetPlayer + "§r yet!"));
                                return 0;
                            }

                            byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, targetContact.public_key);

                            List<String> messageChunks = CryptoManager.chunkMessageAtSpaces(text, 120);

                            for (String chunk : messageChunks) {
                                String encryptedText = CryptoManager.encryptMessage(chunk, sharedSecret);
                                MessageQueueManager.enqueueMessage("!oc_msg " + targetPlayer + " " + targetContact.version + " " + encryptedText);
                            }

                            context.getSource().sendFeedback(Text.literal("§" + format_code_unhighlight1 + "[To " + targetPlayer + "]: §" + format_code_unhighlight2 + text));

                        } catch (Exception e) {
                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to encrypt message!"));
                        }

                        return 1;
                    }))))

                    // Subcommand: /oc delay <ms>
                    .then(ClientCommandManager.literal("delay").then(ClientCommandManager.argument("ms", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 10000)).executes(context -> {
                        String serverId = getCurrentServerId();

                        if (serverId == null) {
                            context.getSource().sendFeedback(Text.literal("§c[Opaque Chat] You must be on a server to set a server-specific delay!"));
                            return 0;
                        }

                        int newDelay = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "ms");
                        ConfigManager.ModConfig.server_cooldowns.put(serverId, newDelay);
                        ConfigManager.saveConfig();

                        context.getSource().sendFeedback(Text.literal("§a[Opaque Chat] Chat delay for " + serverId + " set to " + newDelay + "ms."));
                        return 1;
                    })))

                    // Subcommand: /oc config
                    .then(ClientCommandManager.literal("config").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.send(() -> client.setScreen(YaclScreenBuilder.build(client.currentScreen)));
                        return 1;
                    }))

                    // Subcommand: /oc reload
                    .then(ClientCommandManager.literal("reload").executes(context -> {
                        ConfigManager.loadConfig();
                        context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Configuration reloaded successfully!"));
                        return 1;
                    }))

                    // Subcommand: /oc identity
                    .then(ClientCommandManager.literal("identity")
                            // /oc identity reload
                            .then(ClientCommandManager.literal("reload").executes(context -> {
                                IdentityManager.loadOrGenerateIdentity();
                                context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Identity keys reloaded from disk!"));
                                return 1;
                            }))

                            // /oc identity reset
                            .then(ClientCommandManager.literal("reset").executes(context -> {
                                try {
                                    IdentityManager.resetIdentity();
                                    ContactManager.sessionHistory.clear();

                                    context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Identity reset to v" + IdentityManager.currentIdentity.version + "!"));
                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "Don't worry about old contacts. Your client will auto-heal them when they try to message you."));
                                } catch (Exception e) {
                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to reset identity!"));
                                }
                                return 1;
                            })))

                    // Subcommand: /oc contact
                    .then(ClientCommandManager.literal("contact")

                            // /oc contact reload
                            .then(ClientCommandManager.literal("reload").executes(context -> {
                                ContactManager.loadContacts();
                                context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Contacts list reloaded from disk!"));
                                return 1;
                            }))

                            // /oc contact add <target> (Alias for /oc invite)
                            .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("target", StringArgumentType.word()).suggests((context, builder) -> {
                                if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                                    return CommandSource.suggestMatching(MinecraftClient.getInstance().getNetworkHandler().getPlayerList().stream().map(entry -> entry.getProfile().name()), builder);
                                }
                                return builder.buildFuture();
                            }).executes(context -> {
                                String targetPlayer = StringArgumentType.getString(context, "target");

                                context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Initiating silent handshake with §" + format_code_highlight + targetPlayer + "§r..."));

                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.player != null) {
                                    MessageQueueManager.enqueueMessage("!oc_req " + targetPlayer);
                                }

                                return 1;
                            })))

                            // /oc contact remove <target>
                            .then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("target", StringArgumentType.word()).suggests((context, builder) -> CommandSource.suggestMatching(ContactManager.contacts.keySet(), builder)).executes(context -> {
                                String targetPlayer = StringArgumentType.getString(context, "target").toLowerCase();

                                if (ContactManager.contacts.containsKey(targetPlayer)) {
                                    ContactManager.contacts.remove(targetPlayer);
                                    ContactManager.saveContacts();

                                    ContactManager.sessionHistory.remove(targetPlayer);

                                    context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Secure link with §" + format_code_highlight + targetPlayer + "§r deleted."));
                                } else {
                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] You don't have a saved contact named " + targetPlayer));
                                }
                                return 1;
                            }))))

                    // Subcommand: /oc group
                    .then(ClientCommandManager.literal("group")
                            // /oc group create <name> <targets>
                            .then(ClientCommandManager.literal("create").then(ClientCommandManager.argument("name", StringArgumentType.word()).then(ClientCommandManager.argument("members", StringArgumentType.greedyString()).executes(context -> {
                                String groupName = StringArgumentType.getString(context, "name");
                                String membersRaw = StringArgumentType.getString(context, "members");
                                String[] memberArray = membersRaw.split(" ");

                                java.util.List<String> validMembers = new java.util.ArrayList<>();

                                for (String member : memberArray) {
                                    if (ContactManager.contacts.containsKey(member.toLowerCase())) {
                                        validMembers.add(member);
                                    } else {
                                        context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Skipped " + member + ": Not a saved contact."));
                                    }
                                }

                                if (validMembers.isEmpty()) {
                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Group creation failed. No valid contacts provided."));
                                    return 0;
                                }

                                MinecraftClient client = MinecraftClient.getInstance();
                                validMembers.add(client.getSession().getUsername());
                                nebuli.opaque_chat.data.GroupData newGroup = GroupManager.createGroup(groupName, validMembers);

                                try {
                                    String myPrivateKey = IdentityManager.currentIdentity.private_key;

                                    for (String member : validMembers) {
                                        if (member.equalsIgnoreCase(client.getSession().getUsername())) continue;

                                        ContactData targetContact = ContactManager.contacts.get(member.toLowerCase());
                                        byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, targetContact.public_key);

                                        String encryptedGroupKey = CryptoManager.encryptMessage(newGroup.aes_key_base64, sharedSecret);

                                        MessageQueueManager.enqueueMessage("!oc_ginv " + member + " " + newGroup.uuid + " " + newGroup.version + " " + newGroup.owner + " " + encryptedGroupKey + " " + groupName);
                                    }

                                    context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Group '" + groupName + "' created! Invites are being sent..."));
                                } catch (Exception e) {
                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to encrypt group invites!"));
                                }

                                return 1;
                            }))))

                            .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        java.util.List<String> suggestions = new java.util.ArrayList<>(GroupManager.groups.keySet());
                                        suggestions.addAll(RequestManager.pendingGroupInvites.keySet());
                                        return CommandSource.suggestMatching(suggestions, builder);
                                    })

                                    // /oc group <group name> invite <target>
                                    .then(ClientCommandManager.literal("invite").then(ClientCommandManager.argument("player", StringArgumentType.word()).executes(context -> {
                                        String groupName = StringArgumentType.getString(context, "name");
                                        String targetPlayer = StringArgumentType.getString(context, "player");

                                        nebuli.opaque_chat.data.GroupData targetGroup = GroupManager.groups.values().stream().filter(g -> g.name.equals(groupName)).findFirst().orElse(null);

                                        if (targetGroup == null) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Group not found."));
                                            return 0;
                                        }

                                        String myName = MinecraftClient.getInstance().getSession().getUsername();

                                        if (!targetGroup.owner.equalsIgnoreCase(myName)) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Only the group owner (" + targetGroup.owner + ") can invite new members."));
                                            return 0;
                                        }

                                        ContactData contact = ContactManager.contacts.get(targetPlayer.toLowerCase());
                                        if (contact == null) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] You must be saved contacts with " + targetPlayer + " to invite them."));
                                            return 0;
                                        }

                                        if (targetGroup.members.stream().anyMatch(m -> m.equalsIgnoreCase(targetPlayer))) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] " + targetPlayer + " is already in the group."));
                                            return 0;
                                        }

                                        try {
                                            String myPrivateKey = IdentityManager.currentIdentity.private_key;

                                            byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, contact.public_key);
                                            String encryptedGroupKey = CryptoManager.encryptMessage(targetGroup.aes_key_base64, sharedSecret);

                                            MessageQueueManager.enqueueMessage("!oc_ginv " + targetPlayer + " " + targetGroup.uuid + " " + targetGroup.version + " " + targetGroup.owner + " " + encryptedGroupKey + " " + groupName);

                                            String rosterString = String.join(" ", targetGroup.members);
                                            java.util.List<String> rosterChunks = CryptoManager.chunkMessageAtSpaces(rosterString, 100);

                                            for (String chunk : rosterChunks) {
                                                String encryptedRosterChunk = CryptoManager.encryptMessage(chunk, sharedSecret);
                                                MessageQueueManager.enqueueMessage("!oc_groster " + targetPlayer + " " + targetGroup.uuid + " " + encryptedRosterChunk);
                                            }

                                            MessageQueueManager.enqueueMessage("!oc_gadd " + targetGroup.uuid + " " + targetPlayer);

                                            targetGroup.members.add(targetPlayer);
                                            GroupManager.saveGroups();

                                            context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Invited " + targetPlayer + " to '" + groupName + "'."));
                                        } catch (Exception e) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to encrypt group invite!"));
                                        }

                                        return 1;
                                    })))

                                    // /oc group <group name> accept
                                    .then(ClientCommandManager.literal("accept").executes(context -> {
                                        String groupName = StringArgumentType.getString(context, "name");
                                        nebuli.opaque_chat.data.GroupData pendingGroup = RequestManager.pendingGroupInvites.get(groupName.toLowerCase());
                                        if (pendingGroup != null) {
                                            GroupManager.addGroup(pendingGroup);
                                            RequestManager.pendingGroupInvites.remove(groupName.toLowerCase());
                                            RequestManager.removeRequest("#" + groupName);
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] You securely joined '#" + groupName + "'."));
                                        } else {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] No pending invite found for '#" + groupName + "'."));
                                        }
                                        return 1;
                                    }))

                                    // /oc group <group name> reject
                                    .then(ClientCommandManager.literal("reject").executes(context -> {
                                        String groupName = StringArgumentType.getString(context, "name");
                                        if (RequestManager.pendingGroupInvites.remove(groupName.toLowerCase()) != null) {
                                            RequestManager.removeRequest("#" + groupName);
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat] Rejected invite for '#" + groupName + "'."));
                                        }
                                        return 1;
                                    }))

                                    // /oc group <group name> leave
                                    .then(ClientCommandManager.literal("leave").executes(context -> {
                                        String groupName = StringArgumentType.getString(context, "name");
                                        nebuli.opaque_chat.data.GroupData targetGroup = GroupManager.groups.values().stream()
                                                .filter(g -> g.name.equals(groupName)).findFirst().orElse(null);

                                        if (targetGroup == null) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Group not found."));
                                            return 0;
                                        }

                                        GroupManager.removeGroup(targetGroup.uuid);

                                        MessageQueueManager.enqueueMessage("!oc_gleave " + targetGroup.uuid);

                                        context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] You have left the group '" + groupName + "'."));
                                        return 1;
                                    })))

                                    .then(ClientCommandManager.literal("kick").then(ClientCommandManager.argument("player", StringArgumentType.word()).executes(context -> {
                                        String groupName = StringArgumentType.getString(context, "name");
                                        String targetPlayer = StringArgumentType.getString(context, "player");

                                        String myName = MinecraftClient.getInstance().getSession().getUsername();

                                        nebuli.opaque_chat.data.GroupData targetGroup = GroupManager.groups.values().stream().filter(g -> g.name.equals(groupName)).findFirst().orElse(null);

                                        if (targetGroup == null) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Group not found."));
                                            return 0;
                                        }

                                        if (!targetGroup.owner.equalsIgnoreCase(myName)) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Only the group owner (" + targetGroup.owner + ") can kick members."));
                                            return 0;
                                        }

                                        String exactMemberName = targetGroup.members.stream().filter(m -> m.equalsIgnoreCase(targetPlayer)).findFirst().orElse(null);

                                        if (exactMemberName == null) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Player is not in this group."));
                                            return 0;
                                        }

                                        try {
                                            targetGroup.members.remove(exactMemberName);

                                            byte[] rawAesKey = new byte[16];
                                            new java.security.SecureRandom().nextBytes(rawAesKey);
                                            String newBase64Key = java.util.Base64.getEncoder().encodeToString(rawAesKey);

                                            targetGroup.aes_key_base64 = newBase64Key;
                                            targetGroup.version++;
                                            GroupManager.saveGroups();

                                            String myPrivateKey = IdentityManager.currentIdentity.private_key;

                                            for (String member : targetGroup.members) {
                                                if (member.equalsIgnoreCase(myName)) continue;

                                                ContactData contact = ContactManager.contacts.get(member.toLowerCase());
                                                if (contact != null) {
                                                    byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, contact.public_key);
                                                    String encryptedNewKey = CryptoManager.encryptMessage(newBase64Key, sharedSecret);

                                                    MessageQueueManager.enqueueMessage("!oc_gupd " + member + " " + targetGroup.uuid + " " + targetGroup.version + " " + encryptedNewKey);
                                                }
                                            }

                                            MessageQueueManager.enqueueMessage("!oc_gkicked " + exactMemberName + " " + targetGroup.uuid);

                                            context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Kicked " + exactMemberName + " and rotated AES keys for '" + groupName + "'."));
                                        } catch (Exception e) {
                                            context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to rotate group keys!"));
                                        }

                                        return 1;
                                    })))
                    )
            );
        });
    }
}
