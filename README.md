# Opaque Chat

Opaque Chat is a purely client-side Fabric mod that brings end-to-end encryption to Minecraft chat. Communicate securely with your friends on any server without the server owner, console, or other players understanding a single word of your conversations.

## Features

* **End-to-End Encryption:** Your messages are secured using ECDH (secp256r1) for secure key exchange and AES-GCM for symmetric message encryption.
* **Secure Group Chats:** Create private group chats with Perfect Forward Secrecy and Key Ratcheting to ensure kicked members are mathematically locked out.
* **Message Compression:** All messages are Zlib-compressed before encryption. You can type up to 1280 characters, and the mod will automatically split long messages into secure chunks seamlessly.
* **Seamless GUI Integration:** Features a sleek, animated slide-out panel built directly into the vanilla Chat screen with a custom split-screen layout.
* **Highly Configurable:** Customize UI layouts, colors, and server-specific cooldowns using Yet Another Config Lib (YACL).
* **Client-Side Only:** Works on *any* server, as long as the server allows standard chat messages to be sent and received.

## How It Works

Opaque Chat intercepts incoming and outgoing chat messages prefixed with `!oc_`. 

When you invite a player, the mod performs a silent cryptographic handshake (`!oc_req` and `!oc_key`) to exchange public keys over the server's public chat. Once a shared secret is established, your messages are encrypted (`!oc_msg`) before they ever leave your client. 

Server logs and vanilla players will only see base64-encoded strings (or nothing at all, if they have the mod installed but aren't in the chat), while you and your friends see clean, formatted text. Keys are securely saved in your `config/opaque_chat/` folder and are reused between sessions, meaning you only have to invite a player once!

## Dependencies

* **Fabric Loader & Fabric API**
* **Yet Another Config Lib (YACL)**
* **ModMenu** *(Optional, but recommended for easy config access)*

## Usage

### Opening the Interface
1. Open your standard Minecraft chat (`T` by default).
2. Click the "**OC**" button in the bottom right corner of the screen to access the Opaque Chat interface.

### Starting a Secure Chat
1. **Send an Invite:** Click on a player's name in the GUI and use the action buttons, or type `/oc invite <player>` in chat.
2. **Accept an Invite:** The receiving player will get a notification. They can click `[ACCEPT]` in chat, use the GUI "Requests" tab, or type `/oc accept <player>`.

### Sending Messages
Once a secure link is established:
* Select the contact in the Opaque Chat GUI panel and type your message into the "Secure Message" field.
* Alternatively, use the command `/oc msg <player> <message>`.

## Commands

Opaque Chat operates entirely on the client-side. All commands use the `/oc` prefix.

### General & Configuration
| Command | Description |
| :--- | :--- |
| `/oc help` | Displays the in-game help menu with all available commands. |
| `/oc config` | Opens the visual configuration UI (Requires YACL). |
| `/oc reload` | Reloads `config.json` from your disk. |
| `/oc delay <ms>` | Sets the delay between automated packets for the *current* server to bypass anti-spam kicks (Default: 5000ms). |

### 1-on-1 Secure Messaging
| Command | Description |
| :--- | :--- |
| `/oc invite <player>` | Sends a secure ECC handshake request to the target player. |
| `/oc accept <player>` | Accepts an incoming handshake request and exchanges public keys. |
| `/oc msg <player> <text>` | Encrypts and sends a 1-on-1 message using the shared secret. |

### Contact & Identity Management
| Command | Description |
| :--- | :--- |
| `/oc contact add <player>` | An alias for `/oc invite <player>`. |
| `/oc contact remove <player>`| Deletes a player from your saved contacts list. |
| `/oc contact reload` | Reloads `contacts.json` from your disk. |
| `/oc identity reset` | Generates a new ECC keypair and increments your Key Version. Your contacts will automatically heal and update to your new key the next time you message them. |
| `/oc identity reload` | Reloads `identity.json` from your disk. |

### Group Chats
*Note: The player who creates a group is permanently assigned as the **Group Owner**.*

| Command | Description | Permissions |
| :--- | :--- | :--- |
| `/oc group create <name> <members...>`| Generates a secure AES key, creates the group, and silently distributes the key to the listed members. | Anyone |
| `/oc group invite <name> <player>` | Securely invites a new player to an existing group and automatically syncs the roster for all current members. | Owner Only |
| `/oc group kick <name> <player>` | Kicks a player, generates a **new** AES key (ratcheting the version number), and securely distributes the new key to the remaining members to guarantee Perfect Forward Secrecy. | Owner Only |

---

## Visual Showcase

### Interface Overview
The GUI seamlessly expands from the vanilla chat screen.
![Normal GUI](pictures/pic_1.png)
![Expanded GUI](pictures/gui_1.png)

### Establishing a Connection
When selecting a player you haven't connected with yet, the UI prompts you to initiate a handshake.
![Without key](pictures/gui_2.png)

**Sending an Invite:**
After sending an invite, you will receive a pending notification.
![Notice](pictures/after_invite_pov0.png)

The targeted player receives an interactive invite message.
![Player getting an invite](pictures/after_invite_mod_got_invite.png)

**What others see during the handshake:**
Other players with the mod installed (but not involved in the handshake) will have the raw packets hidden automatically to keep chat clean.
![Player have the mod but not invited](pictures/after_invite_mod_1.png)

Vanilla players without the mod will just see the raw base64 packets.
![Normal player](pictures/after_invite_no_mod.png)

### Accepting the Handshake
When the invited player accepts the invitation, the cryptographic keys are exchanged.
![Your POV](pictures/player_b_accept_1.png)
![The accepted player's POV](pictures/player_b_after_accept.png)

Vanilla players will see the key exchange as chunks of code.
![Haha no mod](pictures/after_accept_no_mod.png)

### Secure Conversations
Once connected, conversations are cleanly separated into the right panel for participants. 
![Conversation, POV: Neb](pictures/conv_neb.png)
![Conversation, POV: playerB](pictures/conv_playerb.png)

Other users with the mod will see nothing, while vanilla players will only see the encrypted AES-GCM payloads.
![Conversation, POV: player with this mod](pictures/conv_playera.png)
![Conversation, POV: player without this mod](pictures/conv_no_mod.png)
