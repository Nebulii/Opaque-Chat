package nebuli.opaque_chat.managers;

import com.mojang.brigadier.arguments.StringArgumentType;
import nebuli.opaque_chat.*;
import nebuli.opaque_chat.chat.ChatInterceptor;
import nebuli.opaque_chat.data.ContactData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import static nebuli.opaque_chat.ConfigManager.ModConfig.*;

public class CommandManager {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // This defines the base command: /oc
            dispatcher.register(ClientCommandManager.literal("oc")

                    // Subcommand: /oc help
                    .then(ClientCommandManager.literal("help")
                            .executes(context -> {
                                context.getSource().sendFeedback(Text.literal("§" + format_code_modname +  "[Opaque Chat]§r Available commands:"));
                                context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc invite <target> §8- Start a secure chat"));
                                context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc invite <target> §8- Accept a secure chat request"));
                                context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc help §8- Show this menu"));
                                context.getSource().sendFeedback(Text.literal("§" + format_code_highlight + "/oc msg <target> <message> §8 Send a encrypted message to <target>"));
                                return 1;
                            })
                    )

                    // Subcommand: /oc invite <target>
                    .then(ClientCommandManager.literal("invite")
                            .then(ClientCommandManager.argument("target", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                                            return CommandSource.suggestMatching(
                                                    MinecraftClient.getInstance().getNetworkHandler().getPlayerList()
                                                            .stream()
                                                            .map(entry -> entry.getProfile().name()),
                                                    builder
                                            );
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String targetPlayer = StringArgumentType.getString(context, "target");

                                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Initiating silent handshake with §" + format_code_highlight + targetPlayer + "§r..."));

                                        MinecraftClient client = MinecraftClient.getInstance();
                                        if (client.player != null) {
                                            client.player.networkHandler.sendChatMessage("!oc_req " + targetPlayer);
                                        }

                                        return 1;
                                    })
                            )
                    )

                    // Subcommand: /oc accept <target>
                    .then(ClientCommandManager.literal("accept")
                            .then(ClientCommandManager.argument("target", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                                            return CommandSource.suggestMatching(
                                                    MinecraftClient.getInstance().getNetworkHandler().getPlayerList()
                                                            .stream()
                                                            .map(entry -> entry.getProfile().name()),
                                                    builder
                                            );
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String target = StringArgumentType.getString(context, "target");
                                        RequestManager.removeRequest(target);
                                        ChatInterceptor.sendPublicKey(target);

                                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat]§r Handshake accepted for §" + format_code_highlight + target));
                                        return 1;
                                    })
                            )
                    )

                    // Subcommand: /oc msg <player> <message>
                    .then(ClientCommandManager.literal("msg")
                            .then(ClientCommandManager.argument("target", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                String targetPlayer = StringArgumentType.getString(context, "target");
                                                String text = StringArgumentType.getString(context, "text");

                                                try {
                                                    String myPrivateKey = IdentityManager.currentIdentity.private_key;
                                                    ContactData targetContact = ContactManager.contacts.get(targetPlayer.toLowerCase());

                                                    if (targetContact == null) {
                                                        context.getSource().sendFeedback(Text.literal("§" + format_code_modname + "[Opaque Chat] You don't have a secure link with §" + format_code_highlight + targetPlayer + " §ryet!"));
                                                        return 0;
                                                    }

                                                    byte[] sharedSecret = CryptoManager.getSharedSecret(myPrivateKey, targetContact.public_key);
                                                    String encryptedText = CryptoManager.encryptMessage(text, sharedSecret);

                                                    MinecraftClient client = MinecraftClient.getInstance();
                                                    if (client.player != null) {
                                                        client.player.networkHandler.sendChatMessage("!oc_msg " + targetPlayer + " " + encryptedText);
                                                        context.getSource().sendFeedback(Text.literal("§" + format_code_unhighlight1 + "[To " + targetPlayer + "]: §" + format_code_unhighlight2 + text));
                                                    }
                                                } catch (Exception e) {
                                                    context.getSource().sendFeedback(Text.literal("§" + format_code_error + "[Opaque Chat] Failed to encrypt message!"));
                                                    e.printStackTrace();
                                                }

                                                return 1;
                                            })
                                    )
                            )
                    )

                    // Subcommand: /oc config
                    .then(ClientCommandManager.literal("config")
                            .executes(context -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                client.send(() -> {
                                    client.setScreen(YaclScreenBuilder.build(client.currentScreen));
                                });
                                return 1;
                            })
                    )

                    // Subcommand: /oc reload
                    .then(ClientCommandManager.literal("reload")
                            .executes(context -> {
                                ConfigManager.loadConfig();
                                context.getSource().sendFeedback(Text.literal("§" + format_code_success + "[Opaque Chat] Configuration reloaded successfully!"));
                                return 1;
                            })
                    )
            );
        });
    }
}
