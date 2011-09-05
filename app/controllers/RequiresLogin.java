package controllers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import play.Logger;
import play.Play;
import play.libs.Mail;
import play.libs.OAuth;
import play.libs.OAuth.ServiceInfo;
import play.mvc.Before;
import play.mvc.Catch;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.templates.JavaExtensions;

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

    public static final ServiceInfo DROPBOX = new ServiceInfo("https://api.dropbox.com/0/oauth/request_token",
                                                              "https://api.dropbox.com/0/oauth/access_token",
                                                              "https://www.dropbox.com/0/oauth/authorize",
                                                              "tkre6hm3z1cvknj", "2hqpa142727u3lr");
    
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

    public static final String ERROR_EMAIL = Play.configuration.getProperty("sortinghat.erroremail", null);
    
    @Catch(Exception.class)
    static void logError(Throwable e) {
        if (ERROR_EMAIL == null) {
            return;
        }

        Logger.error(e, "Sending Gack to %s", ERROR_EMAIL);
        try {
            SimpleEmail email = new SimpleEmail();
            Date date = new Date();
            email.setFrom("sortinghat@heroku.com");
            email.addTo(ERROR_EMAIL);
            email.setSubject(String.format("Error at %s logged: %s", date, e.getClass()));
            StringBuilder body = new StringBuilder();
            body.append("Date:\t").append(date).append("\n\n");
            body.append("Session:\t").append(session.get("username")).append("\n\n");
            body.append("Message:\t").append(e.getMessage()).append("\n\n");
            body.append("Class:\t").append(e.getClass()).append("\n\n");
            body.append("Exception trace: ").append(getSTString(e)).append("\n\n");
            Throwable cause = e.getCause();
            while (cause != null) {
	            body.append("New cause\n\n");
	            body.append("Message:\t").append(cause.getMessage()).append("\n\n");
	            body.append("Class:\t").append(cause.getClass()).append("\n\n");
	            body.append("Exception trace: ").append(getSTString(cause)).append("\n\n");
	            cause = cause.getCause();
            }
            addHttpHeaders(body);
            email.setMsg(body.toString());
            Mail.send(email);
        } catch (EmailException emailEx) {
            Logger.error(emailEx, "Failed to send error email to %s. This is really bad :(", ERROR_EMAIL);
        }
    }
    
    private static void addHttpHeaders(StringBuilder sb) {
        sb.append("HTTP Headers:\n");
        for (Header h: request.headers.values()) {
            sb.append("Header: ").append(h.name).append("\n");
            sb.append("Value: ");
            for (String val: h.values) {
                sb.append(val).append(",");
            }
            sb.append("\n");
        }
        sb.append("\n");
    }

    private static String getSTString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    @Before(unless={"login", "auth", "logout"})
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
	    return "true".equals(session.get("login"));
	}

    public static void auth() throws Exception {
    	flash.keep(REDIRECT_URL);
    	if (flash.contains("verifier")) {
	    	String token = session.get("token");
	    	String secret = session.get("secret");
            OAuth.Response oauthResponse = OAuth.service(DROPBOX).retrieveAccessToken(token, secret);
            if (oauthResponse.error == null) {
                session.put("token", oauthResponse.token);
                session.put("secret", oauthResponse.secret);
                session.put("login", "true");
                redirectToOriginalURL();
            } else {
                Logger.error("Error connecting to Dropbox: " + oauthResponse.error);
                session.remove("token", "secret");
                forbidden("Could not authenticate with Dropbox.");
            }
        } else {
	        OAuth oauth = OAuth.service(DROPBOX);
	        OAuth.Response oauthResponse = oauth.retrieveRequestToken();
	        if (oauthResponse.error == null) {
	        	session.put("token", oauthResponse.token);
	        	session.put("secret", oauthResponse.secret);
	        	flash.put("verifier", "true");
	        	redirect(oauth.redirectUrl(oauthResponse.token) + "&oauth_callback=" + URLEncoder.encode("http://localhost:9000/auth", "UTF-8"));
	        } else {
	            Logger.error("Error connecting to Dropbox: " + oauthResponse.error);
	        }
        }
    }
    
    public static void logout() {
    	session.remove("token", "secret", "login");
    	login();
    }
    
    static void redirectToOriginalURL() {
        String url = flash.get(REDIRECT_URL);
        if(url == null) {
            url = "/";
        }
        redirect(url);
    }
}
