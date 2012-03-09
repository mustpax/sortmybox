package tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import models.User;
import play.Logger;
import siena.Query;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Joiner;

import cron.RuleProcessor;

/**
 * Applies the rules to a chunk of users. 
 * 
 * @author syyang
 */
public class ChunkedRuleProcessor implements Task {
    public static final String CHUNK = "Chunk";
    public static final String LAST_KEY = "LAST_KEY";
    
    public static TaskHandle submit(int chunk, int chunkSize, Long lastId) {
        Queue queue = TaskUtils.getQueue(ChunkedRuleProcessor.class);
        TaskOptions options = TaskUtils.newTaskOptions(ChunkedRuleProcessor.class);
        options.param(ChunkedRuleProcessor.CHUNK, Integer.toString(chunk));
        options.param(RuleProcessor.CHUNK_SIZE, Integer.toString(chunkSize));
        if (lastId != null) {
            options.param(ChunkedRuleProcessor.LAST_KEY, Long.toString(lastId));
        }
        TaskHandle handle = queue.add(options);
        Logger.info("Enq'd new task. Chunk size: %d Task id: %s Last id: %d",
	        		chunkSize, handle.getName(), lastId);
        return handle;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        checkNotNull(context.getParam(CHUNK));
        checkNotNull(context.getParam(RuleProcessor.CHUNK_SIZE));

        int chunk = Integer.valueOf(context.getParam(CHUNK));
        int chunkSize = Integer.valueOf(context.getParam(RuleProcessor.CHUNK_SIZE));
        
        Long lastKey;
        try {
            lastKey = Long.valueOf(context.getParam(LAST_KEY));
        } catch (NumberFormatException ignored) {
            // lastKey is optional, if value is not available
            // we start at the first User
            lastKey = null; 
        }

        List<User> users = getChunkQuery(lastKey, chunkSize).fetch();
        if (users.isEmpty()) {
            Logger.info("Final chunk processed. No more users to enqueue. Last key: %d", lastKey);
        } else {
            Long newLastKey = users.get(users.size() - 1).id;
            Logger.info("Submitting next chunk with last key %d.", newLastKey);
            submit(chunk + 1, chunkSize, newLastKey);
        }

        int moves = 0;
        for (User user : users) {
            moves += user.runRules().size();
        }

        Logger.info("Processed chunk. " +
        		    "Task id: %s Chunk: %d Chunk size: %d Last key: %d Processed users: %d Files moved: %d",
                    context.getTaskId(), chunk, chunkSize, lastKey, users.size(), moves);
    }

    /**
     * @return Query that returns users in increasing id order starting at the
     * last id from the previous chunk (exclusive lower bound)
     */
    public static Query<User> getChunkQuery(Long lastKey, int chunkSize) {
        Query<User> query = User.all().order("id").limit(chunkSize);
        if (lastKey != null) {
            query = query.filter("id >", lastKey);
        }
        return query;    
    }
}
