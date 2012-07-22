package box.gson;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class BoxItemCollection {
    @SerializedName("total_count")
    public int totalCount;

    public List<BoxItem> entries;
}
