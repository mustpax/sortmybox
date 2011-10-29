package controllers;

import java.util.List;
import java.util.Set;

import models.Rule;
import play.mvc.Controller;
import play.mvc.With;

import com.google.appengine.api.datastore.Entity;

import dropbox.Dropbox;
import dropbox.gson.DbxUser;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static String FOLDER = "/Sortbox";

    public static void index() {
        DbxUser user;
        Set<String> files = null;
        if ("true".equals(session.get("offline"))) {
            user = new DbxUser();
            user.uid = 1L;
            user.email = "test@user.com";
            user.name = "Test User";
        } else {
            String token = session.get("token");
            String secret = session.get("secret");
            Dropbox d = new Dropbox(token, secret);
            user = d.getUser();
            files = d.listDir(FOLDER);
        } 
        Iterable<Entity> rules = Rule.getAll();
        render(user, files, rules);
    }
    
    public static void process() {
        checkAuthenticity();
        index();
    }

    public static void proto() {
        render();
    }
}
