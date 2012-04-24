package rules;

import java.util.List;
import java.util.Map;

import play.Logger;

import models.FileMove;
import models.User;

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
import com.google.common.collect.Lists;

import controllers.TaskManager;
import cron.Job;

import tasks.Task;
import tasks.TaskContext;
import tasks.TaskUtils;

/**
 * A scheduled task for applying rules in background.
 * 
 * @author syyang
 */
public class RuleProcessor implements Job {
    /** Indicates how many users to process per task */
    public static final int DEFAULT_CHUNK_SIZE = 20;
    public static final String CHUNK_SIZE = "ChunkSize";

    @Override
    public void execute(Map<String, String> jobData) {
        int chunkSize = jobData.containsKey(CHUNK_SIZE) ?
                Integer.parseInt(jobData.get(CHUNK_SIZE)) : DEFAULT_CHUNK_SIZE;

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Query q = new Query(User.KIND)
            .addFilter("periodicSort", FilterOperator.EQUAL, Boolean.TRUE)
            .addSort(Entity.KEY_RESERVED_PROPERTY)
            .setKeysOnly();
        PreparedQuery pq = ds.prepare(q);
        
        int chunkId = 0;
        int numMessages = 0;
        int offset = 0;
        while (true) {
            FetchOptions options = FetchOptions.Builder
                .withLimit(chunkSize)
                .offset(offset);
            List<Entity> entities = pq.asList(options);
            if (entities.isEmpty()) {
                break;
            }
            long startId = new User(entities.get(0)).id;
            long lastId = new User(entities.get(entities.size() - 1)).id;
            ChunkedRuleProcessor.submit(numMessages++, startId, lastId, chunkSize);
            offset += entities.size();
        }
        Logger.info("Enqueued chunkRuleProcessor messages. Chunk size: %d Num messages: %d",
                chunkSize, chunkId);
    }
    
}
