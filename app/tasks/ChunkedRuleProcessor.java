package tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import models.User;
import play.Logger;
import rules.RuleUtils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

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

        List<User> users = getNextChunk(lastKey, chunkSize);
        if (users.isEmpty()) {
            Logger.info("Final chunk processed. No more users to enqueue. Last key: %d", lastKey);
        } else {
            Long newLastKey = users.get(users.size() - 1).id;
            Logger.info("Submitting next chunk with last key %d.", newLastKey);
            submit(chunk + 1, chunkSize, newLastKey);
        }

        int moves = 0;
        for (User user : users) {
            moves += RuleUtils.runRules(user).size();
        }

        Logger.info("Processed chunk. " +
        		    "Task id: %s Chunk: %d Chunk size: %d Last key: %d Processed users: %d Files moved: %d",
                    context.getTaskId(), chunk, chunkSize, lastKey, users.size(), moves);
    }

    /**
     * @return Query that returns users in increasing id order starting at the
     * last id from the previous chunk (exclusive lower bound)
     */
    public static List<User> getNextChunk(Long lastKey, int chunkSize) {
        Query q = new Query(User.KIND);
        if (lastKey != null) {
            q.addFilter("id", FilterOperator.GREATER_THAN, lastKey);
        }
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(q);
        List<Entity> entities = pq.asList(FetchOptions.Builder.withChunkSize(chunkSize));
        return Lists.transform(entities, User.TO_USER);
    }
}
