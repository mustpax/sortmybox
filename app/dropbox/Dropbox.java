package dropbox;

import play.Play;

public class Dropbox {
    
    public static final String ROOT_DIR = "/";

    public static final String API_URL = "https://api.dropbox.com";
    
    public static final String SITE_URL = "https://www.dropbox.com";
    
    public static final int API_VERSION = 1;

    private static final boolean IS_SANDBOX = 
        Boolean.valueOf(Play.configuration.getProperty("dropbox.sandboxed", "true"));

    private static final String ROOT = IS_SANDBOX ? "sandbox" : "dropbox";
    
    public static boolean isSandbox() {
        return IS_SANDBOX;
    }
    
    public static String getRoot() {
        return ROOT;
    }

    private Dropbox() {}
 } 
