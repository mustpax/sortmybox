package dropbox;

import java.util.regex.Pattern;

import play.Play;
import play.libs.OAuth.ServiceInfo;
import rules.RuleUtils;

public class Dropbox {
    public static final String CONSUMER_KEY = Play.configuration.getProperty("dropbox.consumerKey");
    public static final String CONSUMER_SECRET = Play.configuration.getProperty("dropbox.consumerSecret");

    public static final String API_URL = "https://api.dropbox.com";
    
    public static final String SITE_URL = "https://www.dropbox.com";
    
    public static final int API_VERSION = 1;

    public static String getSortboxPath() {
        return "/SortMyBox";
    }
    
    public static String getOldSortboxPath() {
        return "/Sortbox";
    }

    public static final Pattern DISALLOWED_FILENAME_CHARS = Pattern.compile("[*\\\\:?<>\"|]", Pattern.CASE_INSENSITIVE);

    public static boolean isValidFilename(String name) {
        if (name == null) {
            return true;
        }

        return ! DISALLOWED_FILENAME_CHARS.matcher(RuleUtils.basename(name)).find();
    }

    public static final ServiceInfo OAUTH = new ServiceInfo(DropboxURLs.REQUEST_TOKEN.getPath(),
                                                            DropboxURLs.ACCESS_TOKEN.getPath(),
                                                            DropboxURLs.AUTHORIZATION.getPath(),
                                                            CONSUMER_KEY,
                                                            CONSUMER_SECRET);
    
    private Dropbox() {}
 } 
