package dropbox;

import play.Play;

public class Dropbox {
    public static final String API_URL = "https://api.dropbox.com";
    
    public static final String SITE_URL = "https://www.dropbox.com";
    
    public static final int API_VERSION = 1;

    private static final Root ROOT = Root.valueOf(Play.configuration.getProperty("dropbox.sandboxed", Root.SANDBOX.name()));


    public static enum Root {
        APP("dropbox"),
        SANDBOX("sandbox");

        private final String path;

        private Root(String path) {
            this.path = path;
        }
        public String getPath() { return path; }
        
        public String getSortboxPath() {
            switch(this) {
            case APP:
                return "/Sortbox";
            case SANDBOX:
                return "/";
            }
            throw new IllegalStateException("Unhandled Dropbox root type: " + this);
        }
    }

    public static Root getRoot() {
        return ROOT;
    }

    private Dropbox() {}
 } 
