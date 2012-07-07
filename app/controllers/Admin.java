package controllers;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Random;

import models.CascadingDelete;
import models.DailyUsageStats;
import models.DatastoreUtil;
import models.UsageStats;
import models.User;
import models.User.AccountType;

import org.joda.time.DateTime;
import org.mortbay.log.Log;

import play.Logger;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@With(Login.class)
public class Admin extends Controller {
    private static final int SEARCH_MAX_FETCH_SIZE = 20;

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
                // TODO support Box user lookup
                User userById = null;
                try {
                    Long userId = Long.parseLong(normalized);
                    // TODO - allow admin to specify account type
                    userById = User.findById(AccountType.DROPBOX, userId);
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

    public static void deleteUserPost(String userId) {
        checkAuthenticity();
        
        User user = Login.getUser();

        Key key = null;
        try {
            Long id = Long.valueOf(userId);
            key = User.key(id);
        } catch (NumberFormatException e) {
            key = KeyFactory.createKey(User.KIND, userId);
        }

        // check the user is not currently logged in user
        if (user.getKey().equals(key)) {
            flash.error("Can't delete self: %s", key);
            deleteUser();
        }

        // check the user exists
        User userToDelete = DatastoreUtil.get(key, User.MAPPER);
        if (userToDelete == null) {
            flash.error("User not found. Key %s", key);
            deleteUser();
        }
        
        // delete the user
        CascadingDelete.delete(userToDelete);

        flash.success("Successfully deleted user: %s", userToDelete);
        deleteUser();
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
