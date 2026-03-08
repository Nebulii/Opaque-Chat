package nebuli.opaque_chat.managers;

import java.util.HashSet;
import java.util.Set;

public class RequestManager {
    public static final Set<String> incomingRequests = new HashSet<>();

    public static void addRequest(String username) {
        incomingRequests.add(username.toLowerCase());
    }

    public static void removeRequest(String username) {
        incomingRequests.remove(username.toLowerCase());
    }
}