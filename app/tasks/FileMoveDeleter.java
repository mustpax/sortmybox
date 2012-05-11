package tasks;

import java.util.Date;
import java.util.List;

import models.DatastoreUtil;
import models.FileMove;
import models.User;
import play.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;

public class FileMoveDeleter implements Task {

    private static final String pUSER_ID = "UserId";
    private static final String pEND_DATE = "EndDate";

    private static final int CHUNK_SIZE = 100;
    
    public static TaskHandle submit(User user) {
        Preconditions.checkNotNull(user);
        Date endDate = new Date();
        Queue queue = TaskUtils.getQueue(FileMoveDeleter.class);
        TaskOptions options = TaskUtils.newTaskOptions(FileMoveDeleter.class)
            .param(pUSER_ID, String.valueOf(user.id))
            .param(pEND_DATE, String.valueOf(endDate.getTime()));
        TaskHandle handle = queue.add(options);
        Logger.info("Enqueued delete task. User id: %s End date: %s", user.id, endDate);
        return handle;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        Preconditions.checkNotNull(context.getParam(pUSER_ID));
        Preconditions.checkNotNull(context.getParam(pEND_DATE));
        
        long userId = Long.parseLong(context.getParam(pUSER_ID));
        Date endDate = new Date(Long.parseLong(context.getParam(pEND_DATE)));

        int numChunks = 0;
        while (true) {
            Query query = FileMove.all()
                .addFilter("owner", FilterOperator.EQUAL, userId)
                .setKeysOnly();

            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            PreparedQuery pq = ds.prepare(query);

            List<Entity> entities = pq.asList(FetchOptions.Builder.withLimit(CHUNK_SIZE));
            if (entities == null || entities.isEmpty()) {
                break;
            }

            ds.delete(DatastoreUtil.extractKeys(entities));
            numChunks++;
        }
        
        Logger.info("Finished deleting file moves. User id: %s End date: %s Chunk size: %d Chunks %d", userId, endDate, CHUNK_SIZE, numChunks);
    }

}
