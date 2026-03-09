package nebuli.opaque_chat.data;

public class ContactData {
    public String username;
    public String online_uuid;
    public String offline_uuid;
    public String public_key;
    public int version;

    public ContactData(String username, String online_uuid, String offline_uuid, String public_key, int version) {
        this.username = username;
        this.online_uuid = online_uuid;
        this.offline_uuid = offline_uuid;
        this.public_key = public_key;
        this.version = version;
    }
}