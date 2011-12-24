package controllers;

import java.io.File;
import java.util.Set;

import models.Rule;
import play.Logger;
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
        Iterable<Rule> rules = Rule.getAll();
        render(user, files, rules);
    }
    
    public static void process() {
        checkAuthenticity();

        String token = session.get("token");
        String secret = session.get("secret");
        Dropbox d = new Dropbox(token, secret);
        Set<String> files = d.listDir(FOLDER);
        Iterable<Rule> rules = Rule.getAll();

        for (String file: files) {
            String base = basename(file);
            for (Rule r: rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule key: %s", file, r.dest, r.key);
                    d.move(file, r.dest + "/" + base);
                }
            }
        }

        index();
    }
    
    private static String basename(String path) {
        if (path == null) {
            return null;
        }
        
        File f = new File(path);
        return f.getName();
    }
}
