package dropbox.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a Dropbox user account as represented in the API.
 * @author mustpax
 */
public class DbxAccount {
    public Long uid;
    
    public String email;

    @SerializedName("display_name")
    public String name;

    public boolean notNull() {
        return uid != null;
    }
}
