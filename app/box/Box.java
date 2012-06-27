package box;

import play.Play;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.libs.XPath;

/**
 * Global values for the Box v2 API.
 */
public class Box {
    public static final String API_KEY = Play.mode.isProd() ? Play.configuration.getProperty("box.apiKey") :
                                                              Play.configuration.getProperty("box.apiKeyDev");

    public static String getTicket() {
        WSRequest ws = WS.url("https://www.box.com/api/1.0/rest")
                         .setParameter("action", "get_ticket")
                         .setParameter("api_key", API_KEY);
        HttpResponse resp = ws.get();
        if (resp.success()) {
            return XPath.selectText("//ticket", resp.getXml());
        }

        throw new IllegalStateException("Failed fetching ticket. Status: " + resp.getStatus() +
                                        " Error: " + resp.getString());
    }

    public static String getAuthUrl(String ticket) {
        return "https://www.box.com/api/1.0/auth/" + WS.encode(ticket);
    }

    public static BoxAccount getAccount(String ticket) {
        WSRequest ws = WS.url("https://www.box.com/api/1.0/rest")
                .setParameter("action", "get_auth_token")
                .setParameter("api_key", API_KEY)
                .setParameter("ticket", ticket);

        HttpResponse resp = ws.get();
        if (resp.success()) {
            BoxAccount ret = new BoxAccount();
            ret.token = XPath.selectText("//auth_token", resp.getXml());
            ret.email = XPath.selectText("//email", resp.getXml());
            ret.id = Long.valueOf(XPath.selectText("//user_id", resp.getXml()));
            return ret;
        }

        throw new IllegalStateException("Failed fetching account info. Status: " + resp.getStatus() +
                                        " Error: " + resp.getString());
    }
}
