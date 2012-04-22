package controllers;

import java.util.List;

import org.mortbay.log.Log;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import models.FileMove;
import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import rules.RuleType;
import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.DropboxClient.ListingType;
import dropbox.gson.DbxMetadata;

/**
 * @author mustpax
 */
@With(Login.class)
public class Application extends Controller {
    public static void index() {
        User user = Login.getLoggedInUser();
        boolean didInit = initSortbox(user);
        List<Rule> rules = Lists.newArrayList(Rule.findByOwner(user));
        List<FileMove> moves = Lists.newArrayList(FileMove.findByOwner(user, 10));
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
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Query q = new Query(Rule.KIND).setAncestor(user.getKey());
        int numRules = ds.prepare(q).countEntities(FetchOptions.Builder.withLimit(1));
        if (numRules == 0) {
            // TODO: should we get default rules from a config file?
            List<Rule> rules = Lists.newArrayListWithCapacity(3);
            Transaction tx = ds.beginTransaction();
            try {
                rules.add(new Rule(RuleType.EXT_EQ, "jpg", "/Photos", 0, user.id));
                rules.add(new Rule(RuleType.NAME_CONTAINS, "Essay", "/Documents", 1, user.id));
                rules.add(new Rule(RuleType.GLOB, "Prince*.mp3", "/Music/Prince", 2, user.id));
                ds.put(tx, Lists.transform(rules, new Function<Rule, Entity>() {
                    @Override public Entity apply(Rule rule) {
                        return rule.toEntity(user);
                    }}));
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
            }
        }
    }
}
