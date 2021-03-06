package rules;

import java.util.Arrays;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;

import controllers.ErrorReporter;
import cron.Job;
import models.User;
import play.Logger;
import play.mvc.With;

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
        Key startKey = null;
        Key lastKey = null;

        for (Key userKey: userKeys) {
            if (startKey == null) {
                startKey = userKey;
            }

            lastKey = userKey;
            if (count % chunkSize == 0) {
                ChunkedRuleProcessor.submit(numMessages++, startKey, lastKey, chunkSize);
                startKey = null;
            }
            count++;
        }

        if (startKey != null) {
            ChunkedRuleProcessor.submit(numMessages++, startKey, lastKey, chunkSize);
        }

        Logger.info("Enqueued chunkRuleProcessor messages. Chunk size: %d Num messages: %d",
	                chunkSize, numMessages);
    }
    
    public static Filter getQueryFilter() {
        return new Query.CompositeFilter(CompositeFilterOperator.AND,
                Arrays.<Filter>asList(
                        new Query.FilterPredicate("periodicSort", FilterOperator.EQUAL, true),
                        new Query.FilterPredicate("dropboxV2Migrated", FilterOperator.EQUAL, true)
                        ));
    }

    public static Iterable<Key> getAllUserKeys() {
        Filter f = getQueryFilter();
        Query q = User.all()
                      .setFilter(f)
                      .addSort(Entity.KEY_RESERVED_PROPERTY);
        return User.queryKeys(q);
    }
    
}
