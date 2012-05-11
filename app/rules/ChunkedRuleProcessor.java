package rules;

import java.util.Iterator;

import models.DatastoreUtil;
import models.User;
import play.Logger;
import tasks.Task;
import tasks.TaskContext;
import tasks.TaskUtils;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;

/**
 * Applies the rules to a chunk of users. 
 * 
 * @author syyang
 */
public class ChunkedRuleProcessor implements Task {

    private static final String pCHUNK_ID = "Chunk";
    private static final String pSTART_ID = "StartId";
    private static final String pLAST_ID = "LastId";
    private static final String pCHUNK_SIZE = "ChunkSize";

    public static TaskHandle submit(int chunkId, long startId, long lastId, int chunkSize) {
        Queue queue = TaskUtils.getQueue(ChunkedRuleProcessor.class);
        TaskOptions options = TaskUtils.newTaskOptions(ChunkedRuleProcessor.class)
            .param(pCHUNK_ID, Integer.toString(chunkId))
            .param(pSTART_ID, Long.toString(startId))
            .param(pLAST_ID, Long.toString(lastId))
            .param(pCHUNK_SIZE, Long.toString(chunkSize));
        TaskHandle handle = queue.add(options);
        Logger.info("Enq'd new task. Task id: %s Start id: %d Last id: %d",
	        	    handle.getName(), startId, lastId);
        return handle;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        int chunkId = Integer.valueOf(context.getParam(pCHUNK_ID));
        long startId = Long.valueOf(context.getParam(pSTART_ID));
        long lastId = Long.valueOf(context.getParam(pLAST_ID));
        int chunkSize = Integer.valueOf(context.getParam(pCHUNK_SIZE));

        Iterable<User> users = getUsersForKeyRange(startId, lastId);
        int moves = 0;
        int numUsers = 0;
        for (User user: users) {
            moves += RuleUtils.runRules(user).size();
            numUsers++;
            if (numUsers > chunkSize) {
                Logger.warn("Chunk contains more ids than expected. Task id: %s", context.getTaskId());
                break;
            }
        }

        Logger.info("Processed chunk. Task id: %s Chunk: %d Chunk size: %d Start id: %d Last id: %d Processed users: %d Files moved: %d",
                    context.getTaskId(), chunkId, chunkSize, startId, lastId, numUsers, moves);
    }

    private static Iterable<User> getUsersForKeyRange(long startId, long lastId) {
        Query q = User.all()
			          .addFilter(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, User.key(startId))
			          .addFilter(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN_OR_EQUAL, User.key(lastId))
			          .addFilter("periodicSort", FilterOperator.EQUAL, true);
        return User.query(q);
    }
}
