package dropbox;

import play.Play;

public class Dropbox {
    
    public static final String ROOT_DIR = "/";

    public static final String API_URL = "https://api.dropbox.com";
    
    public static final String SITE_URL = "https://www.dropbox.com";
    
    public static final int API_VERSION = 1;

    private static final Root ROOT = Root.valueOf(Play.configuration.getProperty("dropbox.sandboxed", Root.SANDOBX.name()));


    public static enum Root {
        APP("dropbox"),
        SANDOBX("sandbox");

        private final String path;

        private Root(String path) {
            this.path = path;
        }
        public String getPath() { return path; }
    }

    public static Root getRoot() {
        return ROOT;
    }

    private Dropbox() {}
 } 
