package dropbox.gson;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DbxMetadata {
    public String hash;
    public String path;

    @SerializedName("is_dir")
    public Boolean isDir;
    
    @SerializedName("is_deleted")
    private Boolean isDeleted;
    
    public List<DbxMetadata> contents;
    
    public boolean isDeleted() {
        if (this.isDeleted == null) {
            return false;
        }
        return this.isDeleted;
    }
}
