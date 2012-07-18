package tasks;

import java.util.Date;

import models.DatastoreUtil;
import models.FileMove;
import models.User;
import play.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;

public class FileMoveDeleter implements Task {

    private static final String pEND_DATE = "EndDate";

    public static TaskHandle submit(User user) {
        Preconditions.checkNotNull(user);
        Date endDate = new Date();
        Queue queue = TaskUtils.getQueue(FileMoveDeleter.class);
        TaskOptions options = TaskUtils.newTaskOptions(FileMoveDeleter.class)
            .param("owner", KeyFactory.keyToString(user.getKey()))
            .param(pEND_DATE, String.valueOf(endDate.getTime()));
        TaskHandle handle = queue.add(options);
        Logger.info("Enqueued delete task. User key: %s End date: %s", user.getKey(), endDate);
        return handle;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        Preconditions.checkNotNull(context.getParam("owner"));
        Preconditions.checkNotNull(context.getParam(pEND_DATE));
        
        Key owner = KeyFactory.stringToKey(context.getParam("owner"));
        Date endDate = new Date(Long.parseLong(context.getParam(pEND_DATE)));

        deleteMovesBefore(owner, endDate);

        Logger.info("Finished deleting file moves. Owner: %s End date: %s",
                    owner, endDate);
    }

    public static void deleteMovesBefore(Key owner, Date endDate) {
        Query query = FileMove.all()
            .setAncestor(owner)
            .addFilter("when", FilterOperator.LESS_THAN, endDate)
            .setKeysOnly();

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(query);

        ds.delete(Iterables.transform(pq.asIterable(), DatastoreUtil.TO_KEY));
    }
}
