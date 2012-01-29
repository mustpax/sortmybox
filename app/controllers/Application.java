package controllers;

import java.io.File;
import java.util.*;

import models.Rule;
import models.User;
import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import siena.Query;
import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {

    public static final String BASE_URL = Play.configuration.getProperty("application.baseUrl");

    public static void index() {
        User user = RequiresLogin.getLoggedInUser();
        DropboxClient client = DropboxClientFactory.create(user);
        Set<String> files = client.listDir(Dropbox.getRoot().getSortboxPath());
        List<Rule> rules = Rule.all().fetch();
        render(user, files, rules);
    }
    
    public static void process() {
        checkAuthenticity();

        User user = RequiresLogin.getLoggedInUser();
        DropboxClient client = DropboxClientFactory.create(user);
        Set<String> files = client.listDir(Dropbox.getRoot().getSortboxPath());
        Iterable<Rule> rules = Rule.all().iter();
        
        // TODO return list of moves performed
        for (String file: files) {
            String base = basename(file);
            for (Rule r: rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule id: %s", file, r.dest, r.id);
                    client.move(file, r.dest + "/" + base);
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
