package controllers;

import java.io.File;
import java.util.*;

import com.google.appengine.api.datastore.EntityNotFoundException;

import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import dropbox.Dropbox;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static String FOLDER = "/Sortbox";

    public static void index() {
//          if ("true".equals(session.get("offline"))) {
//          user = new DbxUser();
//          user.uid = 1L;
//          user.email = "test@user.com";
//          user.name = "Test User";
            
        User user = RequiresLogin.getUser();
        Dropbox d = Dropbox.get();
        Set<String> files = d.listDir(FOLDER);
        List<Rule> rules = new ArrayList<Rule>();
        for (Rule r: Rule.getAll()) {
            rules.add(r);
        }
        render(user, files, rules);
    }
    
    public static void process() {
        checkAuthenticity();

        Dropbox d = Dropbox.get();
        Set<String> files = d.listDir(FOLDER);
        Iterable<Rule> rules = Rule.getAll();
        
        // TODO return list of moves performed
        for (String file: files) {
            String base = basename(file);
            for (Rule r: rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule key: %s", file, r.dest, r.key);
                    d.move(file, r.dest + "/" + base);
                    break;
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
