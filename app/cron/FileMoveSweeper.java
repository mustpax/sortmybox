package cron;

import java.util.Date;
import java.util.Map;

import models.FileMove;

import org.joda.time.DateTime;

import play.Logger;
import play.modules.objectify.Datastore;

import com.googlecode.objectify.Key;

/**
 * Deletes all file moves that are older than {@link FileMove#RETENTION_DAYS}.
 */
public class FileMoveSweeper implements Job {

    private static final int CHUNK_SIZE = 100;

    @Override
    public void execute(Map<String, String> jobData) {
        Date oldestPermitted = DateTime.now().minusDays(FileMove.RETENTION_DAYS).toDate();
        int numChunks = 1;
        while (true) {
            Iterable<Key<FileMove>> fileMoveKeys = Datastore
                .query(FileMove.class)
                .filter("when <", oldestPermitted)
                .limit(CHUNK_SIZE)
                .fetchKeys();

            if (!fileMoveKeys.iterator().hasNext()) {
                break;
            }

            Datastore.delete(fileMoveKeys);
            numChunks++;
        }

        Logger.info("Finished sweeping stale file moves. Retention days: %d Chunk size: %d Num chunks: %d",
                FileMove.RETENTION_DAYS, CHUNK_SIZE, numChunks);
    }

}
