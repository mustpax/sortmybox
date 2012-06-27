package controllers;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import models.FileMove;
import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import play.templates.JavaExtensions;
import rules.RuleType;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import common.api.ApiClient;
import common.api.ApiClientFactory;

import dropbox.Dropbox;
import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

/**
 * @author mustpax
 */
@With(Login.class)
public class Application extends Controller {
    /**
     * We want to serialize {@link Date} objects as "x minutes since"
     * so we provide our own Gson serialization adaptor.
     */
    private static class DateSinceSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date d, Type t,
                JsonSerializationContext ctx) {
            return new JsonPrimitive(JavaExtensions.since(d));
        }
    }

    public static final int MAX_FILE_MOVES = 10;
    
    public static void index() {
        User user = Login.getUser();
        InitResult initResult = initSortbox(user);
        List<Rule> rules = Rule.findByUserId(user.id);
        render(user, rules, initResult);
    }

    public static void activity() {
        checkAuthenticity();
        User user = Login.getUser();
        renderJSON(FileMove.findByOwner(user.id, MAX_FILE_MOVES),
                   new DateSinceSerializer());
    }
    
    public static void dirs(String path) {
        checkAuthenticity();
        User u = Login.getUser();
        ApiClient client = ApiClientFactory.create(u);
        try {
	        renderJSON(client.listDir(path, ApiClient.ListingType.DIRS));
        } catch (NotADirectoryException e) {
            Logger.error(e, "User attempt to list a directory which is infact a file: %s", u);
            renderJSON(Collections.emptyList());
        } catch (InvalidTokenException e) {
            Logger.error(e, "Invalid OAuth token for user %s", u);
            Login.logout();
        } catch (IllegalArgumentException e) {
            badRequest();
        }
    }
    
    /**
     * @param user the logged in user
     * @return InitResult the result of initialization
     */
    private static InitResult initSortbox(User user) {
        boolean createdSortboxDir = false;
        boolean createdCannedRules = false;
        boolean updatedSortingFolder = false;
        try {
            ApiClient client = ApiClientFactory.create(user);
            // re-branding requires us to change the sorting folder name
            if (Dropbox.getOldSortboxPath().equals(user.sortingFolder)) {
                // TODO check if folder exists before moving
                // now we need to move the Sortbox folder to SortMyBox
                client.move(Dropbox.getOldSortboxPath(),
	                        Dropbox.getSortboxPath());
                user.sortingFolder = Dropbox.getSortboxPath();
                user.save();
                updatedSortingFolder = true;
            }

            // now get the new sorting folder path for the user and keep going
            // forward
            String sortboxPath = user.sortingFolder;
            if (client.exists(sortboxPath)) {
                // 1. create missing Sortbox folder
                Logger.info("SortMyBox folder missing for user '%s' at path '%s'",
	                        user, sortboxPath);
                createdSortboxDir = client.mkdir(sortboxPath);
                if (createdSortboxDir) {
                    // 2. create canned rules
                    createdCannedRules = createCannedRules(user);
                }
            }
        } catch (InvalidTokenException e) {
            Logger.error(e, "Invalid OAuth token for user %s", user);
            Login.logout();
        } catch (FileMoveCollisionException e) {
            Logger.warn("SortMyBox folder already exists for user '%s'", user);
		}
        return new InitResult(createdSortboxDir, createdCannedRules, updatedSortingFolder);
    }

    /**
     * Creates default set of rules if no rules exist in the Sortbox folder.
     * 
     * @param user the logged in user
     * @return true if canned rules are created
     */
    private static boolean createCannedRules(final User user) {
        if (!Rule.ruleExists(user.id)) {
            List<Rule> rules = Lists.newArrayListWithCapacity(3);
            rules.add(new Rule(RuleType.EXT_EQ, "jpg, png, gif", "/Photos", 0, user.id));
            rules.add(new Rule(RuleType.NAME_CONTAINS, "Essay", "/Documents", 1, user.id));
            rules.add(new Rule(RuleType.GLOB, "Prince*.mp3", "/Music/Prince", 2, user.id));
            Rule.replace(user, rules, null);
            return true;
        }

        return false;
    }
    
    public static class InitResult implements Serializable {
        /** whether the app newly created the Sortbox directory */
        final public boolean createdSortboxDir;

        /** whether the app populated canned rules */
        final public boolean createdCannedRules;
        
        final public boolean updatedSortingFolder;
        
        InitResult(boolean createdSortboxDir, boolean createdCannedRules,boolean updatedSortingFolder) {
            this.createdSortboxDir = createdSortboxDir;
            this.createdCannedRules = createdCannedRules;
            this.updatedSortingFolder = updatedSortingFolder;
        }
    }
}
