package rules;

import java.util.Map;

import models.User;
import play.Logger;
import play.mvc.With;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

import controllers.ErrorReporter;

import cron.Job;

/**
 * A scheduled task for applying rules in background.
 * 
 * @author syyang
 */
@With(ErrorReporter.class)
public class RuleProcessor implements Job {
    /** Indicates how many users to process per task */
    public static final int DEFAULT_CHUNK_SIZE = 20;
    public static final String CHUNK_SIZE = "ChunkSize";

    @Override
    public void execute(Map<String, String> jobData) {
        int chunkSize = jobData.containsKey(CHUNK_SIZE) ?
                Integer.parseInt(jobData.get(CHUNK_SIZE)) : DEFAULT_CHUNK_SIZE;

        Iterable<Key> userKeys = getAllUserKeys();
        
        int numMessages = 0;
        int count = 1;
        Long startId = null;
        Long lastId = null;

        for (Key userKey: userKeys) {
            if (startId == null) {
                startId = userKey.getId();
            }

            lastId = userKey.getId();
            if (count % chunkSize == 0) {
                ChunkedRuleProcessor.submit(numMessages++, startId, lastId, chunkSize);
                startId = null;
            }
            count++;
        }

        if (startId != null) {
            ChunkedRuleProcessor.submit(numMessages++, startId, lastId, chunkSize);
        }

        Logger.info("Enqueued chunkRuleProcessor messages. Chunk size: %d Num messages: %d",
	                chunkSize, numMessages);
    }

    private static Iterable<Key> getAllUserKeys() {
        Query q = User.all()
                      .addFilter("periodicSort", FilterOperator.EQUAL, true)
                      .addSort(Entity.KEY_RESERVED_PROPERTY);
        return User.queryKeys(q);
    }
    
}
