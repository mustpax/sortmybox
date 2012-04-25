package rules;

import java.util.Iterator;
import java.util.Map;

import models.User;
import play.Logger;
import play.modules.objectify.Datastore;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;

import cron.Job;

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

        Iterator<Key<User>> userKeys = getAllUserKeys().iterator();
        
        int numMessages = 0;
        int count = 1;
        Long startId = null;
        Long lastId = null;

        Key<User> userKey;
        while (userKeys.hasNext()) {
            userKey = userKeys.next();
            if (startId == null) {
                startId = lastId = userKey.getId();
            }
            if (count % chunkSize == 0) {
                lastId = userKey.getId();
                ChunkedRuleProcessor.submit(numMessages++, startId, lastId, chunkSize);
                startId = lastId = null;
            }
            count++;
        }
        if (startId != null) {
            ChunkedRuleProcessor.submit(numMessages++, startId, lastId, chunkSize);
        }

        Logger.info("Enqueued chunkRuleProcessor messages. Chunk size: %d Num messages: %d",
                chunkSize, numMessages);
    }

    private static Iterable<Key<User>> getAllUserKeys() {
        return Datastore.query(User.class)
            .filter("periodicSort =", true)
            .order(Entity.KEY_RESERVED_PROPERTY)
            .fetchKeys();
    }
    
}
