package tasks;

import static com.google.common.base.Preconditions.*;
import models.User;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import common.test.TestContext;

import play.Logger;
import play.Play;
import siena.Query;
import cron.RuleProcessor;

/**
 * Applies the rules to a chunk of users. 
 * 
 * @author syyang
 */
public class ChunkedRuleProcessor implements Task {
    
    public static final String pCHUNK = "Chunk";
    public static final String pNUM_CHUNKS = "NumChunks";
    public static final String pKEY_SPACE = "KeySpace";

    public static volatile int KEY_SPACE = -1;
    public static final int DEFAULT_KEY_SPACE = Integer.MAX_VALUE;

    private static final Joiner JOINER = Joiner.on(":").skipNulls();

    @Override
    public void execute(TaskContext context) throws Exception {
        checkNotNull(context.getParam(pCHUNK));
        checkNotNull(context.getParam(pNUM_CHUNKS));

        int chunk = Integer.valueOf(context.getParam(pCHUNK));
        int numChunks = Integer.valueOf(context.getParam(pNUM_CHUNKS));

        ChunkInfo chunkInfo = new ChunkInfo(chunk, numChunks, getKeySpace());
        Query<User> query = getChunkQuery(chunkInfo);

        int moves = 0;
        for (User user : query.iter()) {
            moves += user.runRules().size();
        }

        String message = JOINER.join("ChunkedRuleProcessor", "finished",
                                     context.getTaskId(), chunk, numChunks, moves);
        Logger.info(message);
    }

    public static int getKeySpace() {
        if (TestContext.isRunningTest()) {
            return KEY_SPACE != -1 ? KEY_SPACE : DEFAULT_KEY_SPACE;
        }
        return DEFAULT_KEY_SPACE;
    }

    /**
     * Gets a range query on the user table for range between the 
     * specified lower bound (inclusive) and upper bound (exclusive).
     * The upper bound is ignored if the specified chunk is the last chunk.
     */
    public static Query<User> getChunkQuery(ChunkInfo chunkInfo) {
        Query<User> query = User.all().filter("hash>=", chunkInfo.lowerBound);
        if (!chunkInfo.lastChunk) {
            query.filter("hash<", chunkInfo.upperBound);
        }
        return query;    
    }
    
    public static class ChunkInfo {
        public final int chunkRange;
        public final int lowerBound;
        public final int upperBound;
        public final boolean lastChunk;
        
        public ChunkInfo(int chunk, int numChunks, int keySpace) {
            Preconditions.checkArgument(chunk < numChunks,
                    "the chunk index should be less than the number of chunks");
            this.chunkRange = keySpace / numChunks;
            this.lowerBound = chunk * chunkRange;
            this.upperBound = (chunk + 1) * chunkRange;
            this.lastChunk = chunk + 1 == numChunks;
        }
    }
}
