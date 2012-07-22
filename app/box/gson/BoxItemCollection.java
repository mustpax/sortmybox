package box.gson;

import java.util.List;

import com.google.common.base.Objects;
import com.google.gson.annotations.SerializedName;

public class BoxItemCollection {
    @SerializedName("total_count")
    public int totalCount;

    public List<BoxItem> entries;

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                      .add("totalCount", totalCount)
                      .add("entries", entries)
                      .toString();
    }
}
