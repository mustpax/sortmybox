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
    public static void index() {
        User user = RequiresLogin.getLoggedInUser();
        DropboxClient client = DropboxClientFactory.create(user);
        Set<String> files = client.listDir(Dropbox.getRoot().getSortboxPath());
        List<Rule> rules = Rule.findByOwner(user).fetch();
        render(user, files, rules);
    }
    
    public static void process() {
        checkAuthenticity();
        RequiresLogin.getLoggedInUser().runRules();
        index();
    }
}
