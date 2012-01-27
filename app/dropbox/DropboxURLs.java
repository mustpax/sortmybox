package dropbox;

import com.google.common.base.Joiner;


/**
 * 
 * @author syyang
 */
public enum DropboxURLs {
    
    REQUEST_TOKEN("oauth/request_token"),
    ACCESS_TOKEN("oauth/access_token"),
    AUTHORIZATION(Dropbox.SITE_URL, "oauth/authorize"),
    ACCOUNT("account/info"),
    METADATA("metadata"),
    MOVE("fileops/move")
    ;

    private final String path;
    
    private DropboxURLs(String path) {
        this(Dropbox.API_URL, path);
    }
    
    private DropboxURLs(String baseUrl, String path) {
        this.path = Joiner.on("/").join(baseUrl, Dropbox.API_VERSION, path);
    }
    
    public String getPath() {
        return path;
    }
}
