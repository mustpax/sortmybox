package controllers;

import models.Rule;
import models.User;
import play.Logger;
import play.modules.objectify.Datastore;
import play.mvc.Controller;
import play.mvc.With;
import tasks.FileMoveDeleter;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

@With(Login.class)
public class Accounts extends Controller {
	public static void settingsPost(boolean periodicSort) {
	    checkAuthenticity();
	    User user = Login.getLoggedInUser();
	    user.periodicSort = periodicSort;
	    user.save();
	    if (periodicSort) {
		    flash.success("Periodic sort enabled.");
	    } else {
		    flash.success("Periodic sort disabled.");
	    }
	    Logger.info("Settings updated for user: %s", user);
	    settings();
	}
	
	public static void settings() {
	    User user = Login.getLoggedInUser();
	    render(user);
	}

	public static void delete() {
		User user = Login.getLoggedInUser();
		render(user);
	}
	
    public static void deletePost() {
        checkAuthenticity();
        User user = Login.getLoggedInUser();

        Objectify ofy = Datastore.beginTxn();
        try {
            // 1. delete rules
            Iterable<Key<Rule>> ruleKeys = Datastore.query(Rule.class)
                .ancestor(Datastore.key(User.class, user.id))
                .fetchKeys();
            Datastore.delete(ruleKeys);
            Logger.info("Deleted rules for user: %s", user);

            // 2. delete user
            user.delete();
            Logger.info("Deleted user: %s", user);

            // 3. enqueue a delete task to delete file moves
            FileMoveDeleter.submit(user);

            // 4. commit the txn.
            Datastore.commit();

            session.clear();
            flash.success("Account deleted successfully.");
            Login.login();
        } finally {
            if (ofy.getTxn().isActive()) {
                ofy.getTxn().rollback();
            }
        }
    }

}
