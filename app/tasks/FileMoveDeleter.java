package tasks;

import java.util.Date;

import models.FileMove;
import models.User;
import play.Logger;
import play.modules.objectify.Datastore;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;

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
            Iterable<Key<FileMove>> fileMoveKeys = Datastore
                .query(FileMove.class)
                .ancestor(Datastore.key(User.class, userId))
                .limit(CHUNK_SIZE)
                .fetchKeys();

            if (!fileMoveKeys.iterator().hasNext()) {
                break;
            }

            Datastore.delete(fileMoveKeys);
            numChunks++;
        }
        
        Logger.info("Finished deleting file moves. User id: %s End date: %s Chunk size: %d Chunks %d", userId, endDate, CHUNK_SIZE, numChunks);
    }

}
