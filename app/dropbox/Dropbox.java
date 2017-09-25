package dropbox;

import java.util.Map;
import java.util.regex.Pattern;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.DbxWebAuth.BadRequestException;
import com.dropbox.core.DbxWebAuth.BadStateException;
import com.dropbox.core.DbxWebAuth.CsrfException;
import com.dropbox.core.DbxWebAuth.NotApprovedException;
import com.dropbox.core.DbxWebAuth.ProviderException;

import dropbox.client.DropboxException;
import play.Play;
import play.libs.OAuth.ServiceInfo;
import play.mvc.Scope;
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

    public static final DbxRequestConfig REQ_CONFIG = new DbxRequestConfig("sortbox");
    public static final DbxAppInfo APP_INFO = new DbxAppInfo(CONSUMER_KEY, CONSUMER_SECRET);
    public static final DbxWebAuth WEB_AUTH = new DbxWebAuth(REQ_CONFIG, APP_INFO);
    
    public static String getAuthURL(String redirectUri, Scope.Session session) {
        DbxWebAuth.Request req = DbxWebAuth.newRequestBuilder()
                                           .withRedirectUri(redirectUri,
                                                            new DropboxSessionStore(session))
                                           .build();
        return WEB_AUTH.authorize(req);
    }
    
    public static DbxAuthFinish finishAuth(String redirectUri, Scope.Session session, Map<String, String[]> params) throws NotApprovedException, DropboxException {
        try {
            return WEB_AUTH.finishFromRedirect(redirectUri, new DropboxSessionStore(session), params);
        } catch (BadRequestException | BadStateException | CsrfException | ProviderException | DbxException e) {
            throw new DropboxException(e);
        }
    }
    
    
    
    private Dropbox() {}
 } 
