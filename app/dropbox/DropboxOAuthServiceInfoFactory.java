package dropbox;

import play.Play;
import play.libs.OAuth.ServiceInfo;

public class DropboxOAuthServiceInfoFactory {

    private static final String pCONSUMER_KEY = "dropbox.consumerKey";
    private static final String pCONSUMER_SECRET = "dropbox.consumerSecret";
    
    private static final String CONSUMER_KEY = Play.configuration.getProperty(pCONSUMER_KEY);
    private static final String CONSUMER_SECRET = Play.configuration.getProperty(pCONSUMER_SECRET);
    
    public static ServiceInfo create() {
        return new ServiceInfo(DropboxURLs.REQUEST_TOKEN.getPath(),
                               DropboxURLs.ACCESS_TOKEN.getPath(),
                               DropboxURLs.AUTHORIZATION.getPath(),
                               CONSUMER_KEY,
                               CONSUMER_SECRET);
    }
}
