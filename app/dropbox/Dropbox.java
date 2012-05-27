package dropbox;

import java.util.regex.Pattern;

import play.Play;
import rules.RuleUtils;

public class Dropbox {
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
        return ! DISALLOWED_FILENAME_CHARS.matcher(RuleUtils.basename(name)).find();
    }
    
    private Dropbox() {}
 } 
