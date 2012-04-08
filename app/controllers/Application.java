package controllers;

import java.util.List;

import org.mortbay.log.Log;

import models.FileMove;
import models.Rule;
import models.User;
import play.mvc.Controller;
import play.mvc.With;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.DropboxClient.ListingType;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static void index() {
        User user = RequiresLogin.getLoggedInUser();
        boolean createdSortbox = user.createSortboxIfNecessary();
        List<Rule> rules = Rule.findByOwner(user).fetch();
        List<FileMove> moves = user.getMoves().limit(10).fetch();
        render(user, rules, moves, createdSortbox);
    }
    
    public static void process() {
        checkAuthenticity();
        RequiresLogin.getLoggedInUser().runRules();
        index();
    }
    
    public static void dirs(String path) {
        checkAuthenticity();
        DropboxClient client = DropboxClientFactory.create(RequiresLogin.getLoggedInUser());
        try {
	        renderJSON(client.listDir(path, ListingType.DIRS));
        } catch (IllegalArgumentException e) {
            badRequest();
        }
    }
}
