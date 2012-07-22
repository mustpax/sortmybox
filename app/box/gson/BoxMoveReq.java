package box.gson;

public class BoxMoveReq {
    public BoxItem parent;
    public String name;

    public BoxMoveReq(String parent, String name) {
        this.parent = new BoxItem();
        this.parent.id = parent;
        this.name = name;
    }
}
