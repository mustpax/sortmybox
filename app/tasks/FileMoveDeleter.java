package tasks;

import java.util.Date;
import java.util.List;

import play.Logger;

import models.FileMove;
import models.Rule;
import models.User;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class FileMoveDeleter implements Task {

    private static final String pUSER_ID = "UserId";
    private static final String pEND_DATE = "EndDate";

    private static final int CHUNK_SIZE = 1;

    private static final Function<Entity, Key> TO_KEY = new Function<Entity, Key>() {
        @Override public Key apply(Entity entity) {
            return entity.getKey();
        }
    };
    
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
        
        Key parentKey = KeyFactory.createKey(User.KIND, userId);
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(FileMove.KIND)
            .setAncestor(parentKey)
            .addFilter("when", FilterOperator.LESS_THAN_OR_EQUAL, endDate)
            .setKeysOnly();
        PreparedQuery pq = ds.prepare(q);

        int total = 0;
        while (true) {
            List<Entity> entities = pq.asList(FetchOptions.Builder.withLimit(CHUNK_SIZE));
            if (entities.isEmpty()) {
                break;
            }
            ds.delete(Lists.transform(entities, TO_KEY));
            total += entities.size();
        }
        
        Logger.info("Finished deleting file moves. User id: %s End date: %s Total: %d", userId, endDate, total);
    }

}
