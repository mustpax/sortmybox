package controllers;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Random;

import models.Blacklist;
import models.CascadingDelete;
import models.DailyUsageStats;
import models.DatastoreUtil;
import models.UsageStats;
import models.User;

import org.joda.time.DateTime;
import org.mortbay.log.Log;

import play.Logger;
import play.Play;
import play.libs.WS.HttpResponse;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import dropbox.client.DropboxClientFactory;
import dropbox.client.InvalidTokenException;

@With(Login.class)
public class Admin extends Controller {

    private static final int SEARCH_MAX_FETCH_SIZE = 20;
    private static final int BLACKLIST_MAX_FETCH_SIZE = 100;

    public static void usageStats(boolean fake) {
        if (Play.mode.isDev() && fake) {
            long users = 0L;
            long rules = 30L;
            long fileMoves = 100L;
            for (int i = 0; i < 100; i++) {
                long dUsers = Math.abs(new Random().nextInt()) % 100;
                long dRules = Math.abs(new Random().nextInt()) % 300;
                long dFileMoves = Math.abs(new Random().nextInt()) % 10000;

                users += dUsers;
                rules += dRules;
                fileMoves += dFileMoves;


                UsageStats us = new UsageStats(users, rules, fileMoves);

                DailyUsageStats dus = new DailyUsageStats(dUsers, dRules, dFileMoves);

                Date cur = new DateTime().plusDays(i).toDate();
                us.created = cur;
                us.save();
                dus.created = cur;
                dus.save();
            }
        }

        User user = Login.getUser();
        render(user);
    }

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

        User user = Login.getUser();
        render(user, query, ranSearch, results);
    }

    public static void blacklistedUsers() {
        User user = Login.getUser();
        
        List<Blacklist> blacklist = Lists.newArrayList(Blacklist.query(Blacklist.all(), BLACKLIST_MAX_FETCH_SIZE));
        render(user, blacklist);
    }

    public static void addToBlacklist(String userIdString) {
        User user = Login.getUser();

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
        User user = Login.getUser();
        render(user);
    }

    public static void forceError() {
        throw new IllegalArgumentException("Just pretending to fail.");
    }

    /**
     * Serialize {@link Date} objects via {@link Date#getTime()} (milliseconds since epoch).
     */
    private static class EpochMillisSerializer implements JsonSerializer<Date> {
        @Override
        public JsonElement serialize(Date d, Type t,
                JsonSerializationContext ctx) {
            return new JsonPrimitive(d.getTime());
        }
    }

    public static void stats() {
        checkAuthenticity();

        Query q = UsageStats.all().addSort("created", SortDirection.ASCENDING);
        List<UsageStats> aggrStats = DatastoreUtil.asList(q, UsageStats.MAPPER);

        q = DailyUsageStats.all().addSort("created", SortDirection.ASCENDING);
        List<DailyUsageStats> dailyStats = DatastoreUtil.asList(q, DailyUsageStats.MAPPER);

        renderJSON(ImmutableMap.of("daily", aggrStats, "aggr", dailyStats), new EpochMillisSerializer());
    }

    public static void deleteUserPost(String userIdString) {
        checkAuthenticity();
        
        User user = Login.getUser();

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

    public static void debugApi(Long userId, String url, HTTPMethod method) {
        validation.url(url);
        String apiResp = null;

        if ((userId != null) &&
            (! validation.hasErrors())) {
            checkAuthenticity();
            User u = User.findById(userId);
            if (u == null) {
                validation.addError("user", "Cannot find user.");
            } else {
                try {
                    HttpResponse resp = DropboxClientFactory.create(u).debug(method, url);
                    apiResp = resp.getString();
                } catch (InvalidTokenException e) {
                    Throwables.propagate(e);
                }
            }
        }

        render(userId, url, method, apiResp);
    }

    @Before
    static void checkAccess() {
        User user = Login.getUser();
        if (user == null || ! user.isAdmin()) {
            Logger.warn("Non-admin user attempted to access admin page: %s", user);
            forbidden("Must be admin.");
        }
    }
}
