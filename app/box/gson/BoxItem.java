package box.gson;

import java.io.Serializable;

import com.google.appengine.repackaged.com.google.common.base.Objects;

public class BoxItem implements Serializable {
    public String name;
    public String id;
    
    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                      .add("name", name)
                      .add("id", id)
                      .toString();
    }
}
