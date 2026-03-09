package nebuli.opaque_chat.data;

import java.util.List;

public class GroupData {
    public String uuid;
    public String name;
    public String aes_key_base64;
    public List<String> members;
    public int version;
    public String owner;

    public GroupData(String uuid, String name, String aes_key_base64, List<String> members, int version, String owner) {
        this.uuid = uuid;
        this.name = name;
        this.aes_key_base64 = aes_key_base64;
        this.members = members;
        this.version = version;
        this.owner = owner;
    }
}