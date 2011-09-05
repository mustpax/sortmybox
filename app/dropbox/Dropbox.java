package dropbox;

import com.google.gson.Gson;

import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.WSRequest;
import dropbox.gson.DbxUser;

public class Dropbox {
    public static final ServiceInfo OAUTH = new ServiceInfo("https://api.dropbox.com/0/oauth/request_token",
      "https://api.dropbox.com/0/oauth/access_token",
      "https://www.dropbox.com/0/oauth/authorize",
      "tkre6hm3z1cvknj", "2hqpa142727u3lr");
    
    private String token;
    private String secret;

    public Dropbox(String token, String secret) {
        this.token = token;
        this.secret = secret;
    }

    public DbxUser getUser() {
        WSRequest ws = WS.url("https://api.dropbox.com/0/account/info").oauth(Dropbox.OAUTH, token, secret);
        return new Gson().fromJson(ws.get().getJson(), DbxUser.class);
    }
}
