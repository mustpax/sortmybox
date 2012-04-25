package controllers;

import java.util.List;

import models.FileMove;
import models.Rule;
import models.User;
import play.Logger;
import play.modules.objectify.Datastore;
import play.mvc.Controller;
import play.mvc.With;
import rules.RuleType;

import com.google.common.collect.Lists;
import com.googlecode.objectify.Objectify;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClient.ListingType;
import dropbox.client.DropboxClientFactory;
import dropbox.gson.DbxMetadata;

/**
 * @author mustpax
 */
@With(Login.class)
public class Application extends Controller {
    
    public static final int MAX_FILE_MOVES = 10;
    
    public static void index() {
        User user = Login.getLoggedInUser();
        boolean didInit = initSortbox(user);
        List<Rule> rules = Rule.findByUserId(user.id);
        List<FileMove> moves = Lists.newArrayList(FileMove.findByUser(user).limit(MAX_FILE_MOVES));
        render(user, rules, moves, didInit);
    }
    
    public static void dirs(String path) {
        checkAuthenticity();
        DropboxClient client = DropboxClientFactory.create(Login.getLoggedInUser());
        try {
	        renderJSON(client.listDir(path, ListingType.DIRS));
        } catch (IllegalArgumentException e) {
            badRequest();
        }
    }
    
    /**
     * @param user
     * @return true if a sortbox folder was created
     */
    private static boolean initSortbox(User user) {
        DropboxClient client = DropboxClientFactory.create(user);
        String sortboxPath = Dropbox.getRoot().getSortboxPath();
        DbxMetadata file = client.getMetadata(sortboxPath);
        if (file != null) {
            return false;
        } else {
            // 1. create missing Sortbox folder
            Logger.info("Sortbox folder missing for user '%s' at path '%s'", user, sortboxPath);
            boolean didCreate = client.mkdir(sortboxPath) != null;
            if (didCreate) {
                // 2. create canned rules
                createCannedRules(user);
            }
            return didCreate;
        }
    }

    private static void createCannedRules(final User user) {
        Objectify ofy = Datastore.beginTxn();
        try {
            if (Rule.ruleExists(user.id)) {
                List<Rule> rules = Lists.newArrayListWithCapacity(3);
                rules.add(new Rule(RuleType.EXT_EQ, "jpg", "/Photos", 0, user.id));
                rules.add(new Rule(RuleType.NAME_CONTAINS, "Essay", "/Documents", 1, user.id));
                rules.add(new Rule(RuleType.GLOB, "Prince*.mp3", "/Music/Prince", 2, user.id));
                Datastore.put(rules);
                ofy.getTxn().commit();
            }
        } finally {
            if (ofy.getTxn().isActive()) {
                ofy.getTxn().rollback();
            }
        }
    }
}
