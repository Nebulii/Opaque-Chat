# Opaque-Chat
No watching /msg over console

## The problem
When you use /msg in a server, some server owners can still see your secret message to your friend.
And how do I aim to fix this?
By making an entire encrypted network system inside of vanilla minecraft.

How does this work?
The mod generate a "private key" for each player when they join a server. So player A has keyA, player B has keyB, etc.
When 2 players want to communicate with each other, they can use the [connect command]
Eg:
Player A uses: /connect B
Which will send a generated private key encrypted using keyA. Player B see this and accept the connection request by using command /accept A. Which will use keyB to encrypt the encrypted message from A. The mod on player A's machine will see this, decrypt using keyA and send that key back, which means player B now has the private key.
