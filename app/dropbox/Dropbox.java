package dropbox;

import play.Play;

public class Dropbox {
    public static final String API_URL = "https://api.dropbox.com";
    
    public static final String SITE_URL = "https://www.dropbox.com";
    
    public static final int API_VERSION = 1;

    public static String getSortboxPath() {
        return "/Sortbox";
    }
    
    private Dropbox() {}
 } 
