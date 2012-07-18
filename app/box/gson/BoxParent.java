package box.gson;

public class BoxParent {
    public BoxItem parent;

    public BoxParent(String parent) {
        this.parent = new BoxItem();
        this.parent.id = parent;
    }
}
