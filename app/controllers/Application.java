package controllers;

import java.util.Set;

import com.google.common.collect.Sets;

import play.mvc.Controller;
import play.mvc.With;
import dropbox.Dropbox;
import dropbox.gson.DbxUser;

/**
 * @author mustpax
 */
public class Application extends Controller {
    public static String FOLDER = "/Sortbox";

    public static void index() {
        String token = session.get("token");
        String secret = session.get("secret");
        Dropbox d = new Dropbox(token, secret);
        DbxUser user = d.getUser();
        Set<String> files = d.listDir(FOLDER);
        d.move("/test ?", "/test &");
        render(user, files);
    }
    
    public static void process() {
        checkAuthenticity();
        index();
    }

    public static void proto() {
        render();
    }
}
