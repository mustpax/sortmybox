package dropbox;

import play.libs.OAuth.ServiceInfo;

public class Dropbox {
    public static final ServiceInfo OAUTH = new ServiceInfo("https://api.dropbox.com/0/oauth/request_token",
      "https://api.dropbox.com/0/oauth/access_token",
      "https://www.dropbox.com/0/oauth/authorize",
      "tkre6hm3z1cvknj", "2hqpa142727u3lr");

}
