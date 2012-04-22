package controllers;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Iterables;

import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import tasks.ChunkedRuleProcessor;
import tasks.TaskUtils;

@With(Login.class)
public class Accounts extends Controller {

	private static final String DELETED_ACCOUNT = "deletedAccount";
	
	public static void info() {
		User user = Login.getLoggedInUser();
		render(user);
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
            //Queue queue = TaskUtils.getQueue(AccountDeleteTask.class);
            //TaskOptions options = TaskUtils.newTaskOptions(FileMoveDeleteTask.class);
            //TaskHandle handle = queue.add(options);

            tx.commit();

            session.clear();
            flash.put(DELETED_ACCOUNT, "true");
            Login.login();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
    }

}
