package dropbox.gson;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class DbxMetadata {
    public String hash;
    public String path;

    @SerializedName("is_dir")
    public Boolean isDir;
    
    public List<DbxMetadata> contents;
}
