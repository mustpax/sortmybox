package controllers;

import java.util.Date;

import com.google.appengine.api.datastore.*;
import com.google.common.collect.Iterables;

import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import tasks.ChunkedRuleProcessor;
import tasks.FileMoveDeleter;
import tasks.TaskUtils;

@With(Login.class)
public class Accounts extends Controller {

	public static void info() {
		User user = Login.getLoggedInUser();
		render(user);
	}

	public static void saveSettings(boolean periodicSort) {
	    checkAuthenticity();
	    User user = Login.getLoggedInUser();
	    user.periodicSort = periodicSort;
	    User.update(user);
	    flash.success("Settings saved successfully.");
	    Logger.info("Settings updated for user: %s", user);
	    settings();
	}
	
	public static void settings() {
	    User user = Login.getLoggedInUser();
	    render(user);
	}

	public static void confirmDelete() {
		User user = Login.getLoggedInUser();
		render(user);
	}
	
    public static void delete() {
        User user = Login.getLoggedInUser();

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Transaction tx = ds.beginTransaction();
        try {
            // 1. delete rules
            Query q = new Query(Rule.KIND)
                .setAncestor(user.getKey())
                .setKeysOnly();
            PreparedQuery pq = ds.prepare(tx, q);
            ds.delete(tx, Iterables.transform(pq.asIterable(), Rule.TO_KEY));
            Logger.info("Deleted rules for user: %s", user);

            // 2. delete user
            Entity entity = user.toEntity();
            user.invalidate();
            ds.delete(tx, entity.getKey());
            Logger.info("Deleted user: %s", user);

            // 3. enqueue a delete task to delete file moves
            FileMoveDeleter.submit(user);

            tx.commit();

            session.clear();
            flash.success("Account deleted successfully.");
            Login.login();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
    }

}
