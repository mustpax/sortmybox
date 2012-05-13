package controllers;

import java.util.List;

import models.Blacklist;
import models.CascadingDelete;
import models.User;

import org.mortbay.log.Log;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

@With(Login.class)
public class Admin extends Controller {

    private static final int SEARCH_MAX_FETCH_SIZE = 20;
    private static final int BLACKLIST_MAX_FETCH_SIZE = 100;

    public static void searchUser(String query) {
        boolean ranSearch = false;
        List<User> results = Lists.newArrayList();

        if (query != null) {
            String normalized = query.toLowerCase().trim();
            Log.info("User query: " + query + " Normalized: " + normalized);    
            
            if (!normalized.isEmpty()) {
                ranSearch = true;

                // 1. look up by user id
                User userById = null;
                try {
                    Long userId = Long.parseLong(normalized);
                    userById = User.findById(userId);
                    if (userById != null) {
                        results.add(userById);
                    }
                } catch (NumberFormatException e) {
                    // ignore. the query term is not a user id.
                }

                // 2. look up by user name
                Iterable<User> users =
                    User.query(
                        User.all()
	                        .addFilter("nameLower", FilterOperator.GREATER_THAN_OR_EQUAL, normalized)
	                        .addFilter("nameLower", FilterOperator.LESS_THAN_OR_EQUAL, normalized + "\ufffd"),
	                    SEARCH_MAX_FETCH_SIZE
	                );

                for (User u: users) {
                    // dedup
                    if (userById == null || userById.id != u.id) {
                        results.add(u);
                    }
                }
            }
        }

        User user = Login.getLoggedInUser();
        render(user, query, ranSearch, results);
    }

    public static void blacklistedUsers() {
        User user = Login.getLoggedInUser();
        
        List<Blacklist> blacklist = Lists.newArrayList(Blacklist.query(Blacklist.all(), BLACKLIST_MAX_FETCH_SIZE));
        render(user, blacklist);
    }

    public static void addToBlacklist(String userIdString) {
        User user = Login.getLoggedInUser();

        if (!User.isValidId(userIdString)) {
            flash.error("Invalid user id: " + userIdString);
            blacklistedUsers();

        } 
        
        long userId = Long.valueOf(userIdString);
        if (userId == user.id) {
            flash.error("Can't block self: " + userId);
            blacklistedUsers();
        }
        
        Blacklist blacklist = new Blacklist(userId);
        blacklist.save();
        
        flash.success("Blocked user id: " + userIdString);
        blacklistedUsers();
    }

    public static void removeFromBlacklist(String userIdString) {
        Preconditions.checkArgument(User.isValidId(userIdString),
                "Invalid user id: " + userIdString);
        
        long userId = Long.valueOf(userIdString);
        Blacklist blacklist = Blacklist.findById(userId);
        if (blacklist != null) {
            blacklist.delete();
        }
 
        flash.success("Unblocked user id: " + userIdString);
        blacklistedUsers();
    }

    public static void deleteUser() {
        User user = Login.getLoggedInUser();
        render(user);
    }

    public static void forceError() {
        throw new IllegalArgumentException("Just pretending to fail.");
    }

    public static void deleteUserPost(String userIdString) {
        checkAuthenticity();
        
        User user = Login.getLoggedInUser();

        if (!User.isValidId(userIdString)) {
            flash.error("Invalid user id: " + userIdString);
            deleteUser();
        }
        
        long userId = Long.valueOf(userIdString);
        
        // check the user is not currently logged in user
        if (user.id == userId) {
            flash.error("Can't delete self: %s", userIdString);
            deleteUser();
        }

        // check the user exists
        User userToDelete = User.findById(userId);
        if (userToDelete == null) {
            flash.error("Non-existant user: User id: %s", userIdString);
            deleteUser();
        }
        
        // delete the user
        CascadingDelete.delete(userToDelete);

        flash.success("Successfully deleted user: %s", userIdString);
        deleteUser();
    }

    @Before
    static void checkAccess() {
        User user = Login.getLoggedInUser();
        if (user == null || ! user.isAdmin()) {
            Logger.warn("Non-admin user attempted to access admin page: %s", user);
            forbidden("Must be admin.");
        }
    }

}
