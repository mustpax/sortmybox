package dropbox;

import java.util.Set;

import models.User;

import oauth.signpost.OAuth;

import play.Logger;
import play.Play;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import controllers.RequiresLogin;

import dropbox.gson.DbxMetadata;
import dropbox.gson.DbxUser;

public class Dropbox {
	
	private static final String pCONSUMER_KEY = "dropbox.consumerKey";
	private static final String pCONSUMER_SECRET = "dropbox.consumerSecret";
	
    public static final ServiceInfo OAUTH = new ServiceInfo("https://api.dropbox.com/0/oauth/request_token",
													        "https://api.dropbox.com/0/oauth/access_token",
													        "https://www.dropbox.com/0/oauth/authorize",
													        Play.configuration.getProperty(pCONSUMER_KEY),
													        Play.configuration.getProperty(pCONSUMER_SECRET));
    
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
    
    /**
     * Get all files, excluding directories, inside the given directory.
     * 
     * @param path path to the directory to check with the leading /
     * @return set of files (not directories) inside the directory
     */
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
	            if (! entry.isDir) {
	                ret.add(entry.path);
	            }
	        }
	        return ret;
        } else {
            Logger.error("Failed listing '%s'. %s", path, getError(resp));
            return null;
        }
    }
    
    public DbxMetadata move(String from, String to) {
        if (from == null || to == null) {
            throw new NullPointerException("To and from paths cannot be null.");
        }

        if ((from.charAt(0) != '/') || (to.charAt(0) != '/')) {
            throw new IllegalArgumentException("To and from paths should start with /");
        }
        
        WSRequest ws = WS.url(String.format("https://api.dropbox.com/0/fileops/move?from_path=%s&to_path=%s&root=dropbox",
                                            encodeParam(from),
                                            encodeParam(to)))
				         .oauth(Dropbox.OAUTH, token, secret);
        HttpResponse resp = ws.post();
        if (resp.success()) {
            Logger.info("Successfully moved files. From: '%s' To: '%s'", from, to);
	        return new Gson().fromJson(resp.getJson(), DbxMetadata.class);
        }
        Logger.warn("Failed to move files. Error: %s", getError(resp));
        return null;
    }
    
    /**
     * @return the correct Dropbox instance for the currently logged in user
     */
    public static Dropbox get() {
        User u = RequiresLogin.getUser();
        return new Dropbox(u.getToken(), u. getSecret());
    }
    
    private static String getError(HttpResponse resp) {
        return resp.getJson().getAsJsonObject().get("error").getAsString();
    }
    
    
    /**
     * WS.oauth() signing does not play nice with full URL encoded
     * file paths. So we have to use %20 instead of + and not 
     * escape "/" seperators
     */
    private static String encodePath(String param) {
        // XXX this will incorrectly unescape %%2F
        return encodeParam(param).replaceAll("%2F", "/");
    }

    private static String encodeParam(String param) {
        // XXX should we toLowerCase here?
        return OAuth.percentEncode(param.toLowerCase());
    }
 } 
