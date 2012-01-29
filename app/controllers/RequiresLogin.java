package controllers;

import java.net.URLEncoder;
import java.util.Arrays;

import com.google.common.base.Joiner;
import common.request.Headers;

import models.User;

import play.Logger;
import play.Play;
import play.libs.OAuth;
import play.libs.OAuth.ServiceInfo;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.templates.JavaExtensions;
import dropbox.Dropbox;
import dropbox.DropboxOAuthServiceInfoFactory;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.gson.DbxAccount;

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
    private static final String SESSION_USER = "userid";

    private static class SessionKeys {
        static final String TOKEN = "token";
        static final String SECRET = "secret";
        static final String LOGIN = "login";
        static final String OFFLINE = "offline";
        static final String UID = "uid";
    }
    
    @Before(priority=10)
    static void https() {
        if (request.headers.containsKey(Headers.FORWARDED_FOR)) {
            request.remoteAddress = Headers.first(request, Headers.FORWARDED_FOR);
        }

        if (request.headers.containsKey(Headers.FORWARDED_PROTO)) {
            Header h = request.headers.get(Headers.FORWARDED_PROTO);
            request.secure = "https".equals(h.value());
        }
    }

    @Before
    static void log() {
        Joiner joiner = Joiner.on(":").skipNulls();
        Logger.info(joiner.join(request.remoteAddress,
                                request.method,
                                request.secure ? "ssl" : "http",
                                session.get(SESSION_USER),
                                request.url));
    }

    @Before(unless={"login", "auth", "logout", "offline"})
    static void checkAccess() throws Throwable {
        if (!isLoggedIn()) {
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
            if (!hasProfile) {
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
        return "true".equals(session.get(SessionKeys.LOGIN)) || 
               "true".equals(session.get("offline"));
    }

    public static void auth() throws Exception {
        flash.keep(REDIRECT_URL);
        if (flash.contains("verifier")) {
            String token = session.get(SessionKeys.TOKEN);
            String secret = session.get(SessionKeys.SECRET);
            ServiceInfo serviceInfo = DropboxOAuthServiceInfoFactory.create();
            OAuth.Response oauthResponse = OAuth.service(serviceInfo).retrieveAccessToken(token, secret);
            if (oauthResponse.error == null) {
                Logger.info("Succesfully authenticated with Dropbox.");
                session.put(SessionKeys.LOGIN, "true");
                User u = upsertUser(oauthResponse.token, oauthResponse.secret);
                session.put(SessionKeys.UID, u.id);
                session.remove(SessionKeys.TOKEN, SessionKeys.SECRET);
                redirectToOriginalURL();
            } else {
                Logger.error("Error connecting to Dropbox: " + oauthResponse.error);
                session.remove(SessionKeys.TOKEN, SessionKeys.SECRET);
                forbidden("Could not authenticate with Dropbox.");
            }
        } else {
            ServiceInfo serviceInfo = DropboxOAuthServiceInfoFactory.create();
            OAuth oauth = OAuth.service(serviceInfo);
            OAuth.Response oauthResponse = oauth.retrieveRequestToken();
            if (oauthResponse.error == null) {
                session.put(SessionKeys.TOKEN, oauthResponse.token);
                session.put(SessionKeys.SECRET, oauthResponse.secret);
                flash.put("verifier", "true");
                redirect(oauth.redirectUrl(oauthResponse.token) +
                         "&oauth_callback=" +
                        URLEncoder.encode(request.getBase()+"/auth", "UTF-8"));
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
        DropboxClient client = DropboxClientFactory.create(token, secret);
        DbxAccount account = client.getAccount();
        return User.findOrCreateByDbxAccount(account, token, secret);
    }
    
    /**
     * @return the currently logged in user, null if no logged in user
     */
    public static User getLoggedInUser() {
        String uid = session.get(SessionKeys.UID);
        try {
            User user = User.findById(Long.valueOf(uid));
            if (user == null) {
                Logger.warn("User missing. Uid: %s", uid);
            }
            return user;
        } catch (NumberFormatException e) {
            Logger.error(e, "Invalidly formatted or missing uid: %s", uid);
            return null;
        }
    }

    public static void logout() {
        session.remove(SessionKeys.TOKEN, SessionKeys.SECRET,
                SessionKeys.LOGIN, SessionKeys.OFFLINE);
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
        session.put(SessionKeys.OFFLINE, "true");
        Application.index();
    }
}
