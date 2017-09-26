package rules;

import models.User;
import play.Logger;
import play.Play;
import tasks.Task;
import tasks.TaskContext;
import tasks.TaskUtils;

import java.util.Arrays;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
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
    public static final String pSTART_ID = "StartId";
    public static final String pLAST_ID = "LastId";
    private static final String pCHUNK_SIZE = "ChunkSize";

    public static TaskHandle submit(int chunkId, Key startKey, Key lastKey, int chunkSize) {
        Queue queue = TaskUtils.getQueue(ChunkedRuleProcessor.class);
        TaskOptions options = TaskUtils.newTaskOptions(ChunkedRuleProcessor.class)
            .param(pCHUNK_ID, Integer.toString(chunkId))
            .param(pSTART_ID, KeyFactory.keyToString(startKey))
            .param(pLAST_ID, KeyFactory.keyToString(lastKey))
            .param(pCHUNK_SIZE, Long.toString(chunkSize));
        TaskHandle handle = queue.add(options);
        Logger.info("Enq'd new task. Task id: %s Start key: %s Last key: %s",
	        	    handle.getName(), startKey, lastKey);
        return handle;
    }

    @Override
    public void execute(TaskContext context) throws Exception {
        if (Play.runingInTestMode()) {
            Logger.error("Not running rule because we're in test mode.");
            return;
        }

        int chunkId = Integer.valueOf(context.getParam(pCHUNK_ID));
        Key startKey = KeyFactory.stringToKey(context.getParam(pSTART_ID));
        Key lastKey = KeyFactory.stringToKey(context.getParam(pLAST_ID));
        int chunkSize = Integer.valueOf(context.getParam(pCHUNK_SIZE));

        Iterable<User> users = getUsersForKeyRange(startKey, lastKey);
        int moves = 0;
        int numUsers = 0;
        for (User user: users) {
            moves += RuleUtils.runRules(user).size();
            numUsers++;
            if (numUsers > chunkSize) {
                Logger.error("Chunk contains more ids than expected. Task id: %s", context.getTaskId());
                break;
            }
        }

        Logger.info("Processed chunk. Task id: %s Chunk: %d Chunk size: %d Start key: %s Last key: %s Processed users: %d Files moved: %d",
                    context.getTaskId(), chunkId, chunkSize, startKey, lastKey, numUsers, moves);
    }

    public static Iterable<User> getUsersForKeyRange(Key startKey, Key lastKey) {
        Query.Filter f = new Query.CompositeFilter(CompositeFilterOperator.AND,
                Arrays.<Query.Filter>asList(
                            RuleProcessor.getQueryFilter(),
                            new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, startKey),
                            new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN_OR_EQUAL, lastKey)
                        ));
        Query q = User.all()
                      .setFilter(f);
        return User.query(q);
    }
}
