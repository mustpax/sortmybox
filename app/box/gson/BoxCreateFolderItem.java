package box.gson;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class BoxCreateFolderItem implements Serializable {
    public String name;
    public BoxItem parent;
    
    public BoxCreateFolderItem(String name, BoxItem parent) { 
        this.name = name;
        this.parent = parent;
    }
}
