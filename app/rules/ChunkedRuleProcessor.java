package rules;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.List;

import models.User;
import play.Logger;
import tasks.Task;
import tasks.TaskContext;
import tasks.TaskUtils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

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
        TaskOptions options = TaskUtils.newTaskOptions(ChunkedRuleProcessor.class);
        options.param(pCHUNK_ID, Integer.toString(chunkId));
        options.param(pSTART_ID, Long.toString(startId));
        options.param(pLAST_ID, Long.toString(lastId));
        options.param(pCHUNK_SIZE, Long.toString(chunkSize));
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

        Key startKey = KeyFactory.createKey(User.KIND, startId);
        Key lastKey = KeyFactory.createKey(User.KIND, lastId);

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Query q = new Query(User.KIND)
            .addFilter(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, startKey)
            .addFilter(Entity.KEY_RESERVED_PROPERTY, FilterOperator.LESS_THAN_OR_EQUAL, lastKey)
            .addFilter("periodicSort", FilterOperator.EQUAL, Boolean.TRUE);
        PreparedQuery pq = ds.prepare(q);
        List<Entity> entities = pq.asList(FetchOptions.Builder.withLimit(chunkSize));
        List<User> users = Lists.transform(entities, User.TO_USER);
        int moves = 0;
        for (User user : users) {
            moves += RuleUtils.runRules(user).size();
        }

        Logger.info("Processed chunk. Task id: %s Chunk: %d Chunk size: %d Start id: %d Last id: %d Processed users: %d Files moved: %d",
                    context.getTaskId(), chunkId, chunkSize, startId, lastId, users.size(), moves);
    }
}
