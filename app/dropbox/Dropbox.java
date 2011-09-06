package dropbox;

import java.util.Set;

import play.Logger;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import dropbox.gson.DbxMetadata;
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
    
    public Set<String> listDir(String path) {
        if (path == null) {
            throw new NullPointerException("Path missing.");
        }

        if (path.charAt(0) != '/') {
            throw new IllegalArgumentException("Path should start with /");
        }

        WSRequest ws = WS.url("https://api.dropbox.com/0/metadata/dropbox" + encodePath(path))
				         .oauth(Dropbox.OAUTH, token, secret);
        HttpResponse resp = ws.get();
        if (resp.success()) {
	        DbxMetadata metadata = new Gson().fromJson(resp.getJson(), DbxMetadata.class);
	        if (! metadata.isDir) {
	            throw new IllegalArgumentException("Expecting dir, got a file: " + path);
	        }

	        Set<String> ret = Sets.newHashSet();
	        for (DbxMetadata entry: metadata.contents) {
	            ret.add(entry.path);
	        }
        return ret;
        } else {
            Logger.error(resp.getJson().getAsJsonObject().get("error").getAsString());
            return null;
        }
    }
    
    /**
     * WS.oauth() signing does not play nice with full URL encoded
     * file paths. So we have to use %20 instead of + and not 
     * escape "/" seperators
     */
    public static String encodePath(String param) {
        return encodeParam(param).replaceAll("%2F", "/");
    }

    public static String encodeParam(String param) {
        return WS.encode(param.toLowerCase()).replaceAll("\\+", "%20");
    }
 } 
