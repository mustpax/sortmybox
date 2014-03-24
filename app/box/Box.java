package box;

import java.net.URLEncoder;

import com.google.common.base.Joiner;
import com.google.gson.Gson;

import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.libs.XPath;
import play.mvc.Router;

/**
 * Global values for the Box v2 API.
 */
public class Box {
    public static final String CLIENT_ID = Play.mode.isProd() ? Play.configuration.getProperty("box.apiKey") :
                                                                Play.configuration.getProperty("box.apiKeyDev");
    public static final String CLIENT_SECRET = Play.mode.isProd() ? Play.configuration.getProperty("box.apiKeySecret") :
                                                                    Play.configuration.getProperty("box.apiKeyDevSecret");

    
    public static class URLs {
        public static final String BASE_V2_OAUTH = "https://www.box.com/api/oauth2";

        public static final String BASE_V2 = "https://api.box.com/2.0";
    }
    
    public static String getAuthUrl() {
        return URLs.BASE_V2_OAUTH +
                String.format("/authorize?response_type=code&client_id=%s&redirect_uri=%s",
                              CLIENT_ID, WS.encode(Router.getFullUrl("Login.boxAuthCallback")));
    }

    public static BoxCredentials getCred(String code) {
        WSRequest ws = WS.url(URLs.BASE_V2_OAUTH + "/token")
                         .setParameter("grant_type", "authorization_code")
                         .setParameter("code", code)
                         .setParameter("client_id", CLIENT_ID)
                         .setParameter("client_secret", CLIENT_SECRET);

        HttpResponse resp = ws.post();
        if (resp.success()) {
            return new Gson().fromJson(resp.getJson(), BoxCredentials.class);
        }

        throw new IllegalStateException("Failed fetching box account credentials. Status: " + resp.getStatus() +
                                        " Error: " + resp.getString());
    }
    
}
