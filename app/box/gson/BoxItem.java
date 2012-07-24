package box.gson;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class BoxItem implements Serializable {
    public static final String FILE = "file";
    public static final String FOLDER = "folder";

    public String name;
    public String id;
    public String type;
    public String path;

    @SerializedName("item_collection")
    public BoxItemCollection children;

    public boolean isFolder() {
        return FOLDER.equals(type);
    }

    public boolean isFile() {
        return FILE.equals(type);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                      .add("name", name)
                      .add("id", id)
                      .add("type", type)
                      .add("path", path)
                      .add("children", children)
                      .toString();
    }
}
