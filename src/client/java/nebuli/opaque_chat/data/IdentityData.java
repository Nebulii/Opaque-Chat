package nebuli.opaque_chat.data;

public class IdentityData {
    public String username;
    public String online_uuid;
    public String offline_uuid;
    public String public_key;
    public String private_key;
    public int version;

    public IdentityData(String username, String online_uuid, String offline_uuid, String public_key, String private_key, int version) {
        this.username = username;
        this.online_uuid = online_uuid;
        this.offline_uuid = offline_uuid;
        this.public_key = public_key;
        this.private_key = private_key;
        this.version = version;
    }
}