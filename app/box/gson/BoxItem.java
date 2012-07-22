package box.gson;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class BoxItem implements Serializable {
    public static enum Type {
        file,
        folder
    }

    public String name;
    public String id;
    public Type type;
    public String path;

    @SerializedName("item_collection")
    public BoxItemCollection children;

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
