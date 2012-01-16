package controllers;

import java.net.URLEncoder;
import java.util.Arrays;

import com.google.appengine.api.datastore.EntityNotFoundException;

import models.User;

import play.Logger;
import play.Play;
import play.libs.OAuth;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.templates.JavaExtensions;
import dropbox.Dropbox;
import dropbox.gson.DbxUser;

/**
 * Make given controller or controller methods require login.
 * Usage:
 * @With(RequiresLogin.class)
 * 
 * Based on {@link Secure}.
 *
 * @author mustpax
 */
public class RequiresLogin extends Controller {
    private static final String REDIRECT_URL = "url";

    public static final String SESSION_USER = "userid";
    
    @Before(priority=10)
    static void https() {
    	if (request.headers.containsKey("x-forwarded-for")) {
    		Header h = request.headers.get("x-forwarded-for");
    		request.remoteAddress = h.value();
    	}

    	if (request.headers.containsKey("x-forwarded-proto")) {
    		Header h = request.headers.get("x-forwarded-proto");
    		request.secure = "https".equals(h.value());
    	}
    }
	
    @Before
    static void log() {
    	Logger.info(JavaExtensions.join(Arrays.asList(request.remoteAddress,
    												  request.method,
    												  request.secure ? "http" : "ssl",
    												  session.get(SESSION_USER),
    												  request.url), ":"));
    }

    @Before(unless={"login", "auth", "logout", "offline"})
    static void checkAccess() throws Throwable {
        if (! isLoggedIn()) {
            if ("GET".equals(request.method)) {
            	flash.put(REDIRECT_URL, request.url);
            }

            login();
        }

        // Checks
        Check check = getActionAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if(check != null) {
            check(check);
        }
    }
    
	private static void check(Check check) {
        for(String profile : check.value()) {
            boolean hasProfile = hasProfile(profile);
            if (! hasProfile) {
            	forbidden("Forbidden. User missing profile: " + profile);
            }
        }
	}

	private static boolean hasProfile(String profile) {
	    // TODO
		return false;
	}

	public static void login() {
		if (isLoggedIn()) {
			Logger.info("User visited login url, but already logged in.");
			redirectToOriginalURL();
		} else {
			flash.keep(REDIRECT_URL);
			render();
		}
	}

	public static boolean isLoggedIn() {
	    return "true".equals(session.get("login")) || 
	           "true".equals(session.get("offline"));
	}

    public static void auth() throws Exception {
    	flash.keep(REDIRECT_URL);
    	if (flash.contains("verifier")) {
	    	String token = session.get("token");
	    	String secret = session.get("secret");
            OAuth.Response oauthResponse = OAuth.service(Dropbox.OAUTH).retrieveAccessToken(token, secret);
            if (oauthResponse.error == null) {
                Logger.info("Succesfully authenticated with Dropbox.");
                session.put("login", "true");
                User u = upsertUser(oauthResponse.token, oauthResponse.secret);
                session.put("uid", u.getUId());
                session.remove("token", "secret");
                redirectToOriginalURL();
            } else {
                Logger.error("Error connecting to Dropbox: " + oauthResponse.error);
                session.remove("token", "secret");
                forbidden("Could not authenticate with Dropbox.");
            }
        } else {
	        OAuth oauth = OAuth.service(Dropbox.OAUTH);
	        OAuth.Response oauthResponse = oauth.retrieveRequestToken();
	        if (oauthResponse.error == null) {
	        	session.put("token", oauthResponse.token);
	        	session.put("secret", oauthResponse.secret);
	        	flash.put("verifier", "true");
	        	redirect(oauth.redirectUrl(oauthResponse.token) +
                         "&oauth_callback=" +
                         URLEncoder.encode(Play.mode.isDev() ? 
                                           "http://localhost:9000/auth" : 
                                           "http://sort-box.appspot.com/auth", "UTF-8"));
	        } else {
	            Logger.error("Error connecting to Dropbox: " + oauthResponse.error);
	            error("Error connecting to Dropbox.");
	        }
        }
    }
    
    /**
     * Ensure that the Dropbox user authenticated with the given oauth credentials
     * is in the datastore.
     */
    private static User upsertUser(String token, String secret) {
        Dropbox d = new Dropbox(token, secret);
        DbxUser dbxUser = d.getUser();
        return User.getOrCreate(dbxUser, token, secret);
    }
    
    /**
     * @return the currently logged in user, null if no logged in user
     */
    public static User getUser() {
        String uid = session.get("uid");
        try {
            return User.get(Long.valueOf(uid));
        } catch (NumberFormatException e) {
            Logger.error(e, "Invalidly formatted or missing uid: %s", uid);
            return null;
        } catch (EntityNotFoundException e) {
            Logger.error(e, "User missing. Uid: %s", uid);
            return null;
        }
    }

    public static void logout() {
    	session.remove("token", "secret", "login", "offline");
    	login();
    }
    
    static void redirectToOriginalURL() {
        String url = flash.get(REDIRECT_URL);
        if(url == null) {
            url = "/";
        }
        redirect(url);
    }

    /**
     * Allow development when Dropbox is unreachable by creating a dummy user.
     */
    public static void offline() {
        assert Play.mode.isDev();
        session.put("offline", "true");
        Application.index();
    }
}
