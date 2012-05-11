package cron;

import java.util.Date;
import java.util.Map;

import models.DatastoreUtil;
import models.FileMove;
import models.Rule;
import models.UsageStats;
import models.User;

import org.joda.time.DateTime;

import play.Logger;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.QueryResultList;

public class UsageStatsGatherer implements Job {

    private static final int MAX_BATCH = 500;

    @Override
    public void execute(Map<String, String> jobData) {
        int numUsers = DatastoreUtil.count(User.all());
        int numRules = DatastoreUtil.count(Rule.all());
        
        Date to = DateTime.now().toDateMidnight().toDate();
        Date from = DateTime.now().minusDays(1).toDateMidnight().toDate();

        int numFileMoves = countFileMoves(from, to);
        
        UsageStats stats = new UsageStats(numUsers, numRules, numFileMoves);
        stats.save();
        
        Logger.info("Finished collecting user stats: " + stats);
    }
    
    private static int countFileMoves(Date from, Date to) {
        int count = 0;
        Cursor cursor = null;
        while (true) {
            Query query = new Query(FileMove.class.getSimpleName())
                .addFilter("when", FilterOperator.GREATER_THAN_OR_EQUAL, from)
                .addFilter("when", FilterOperator.LESS_THAN_OR_EQUAL, to)
                .setKeysOnly();
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            PreparedQuery pq = ds.prepare(query);

            FetchOptions fetchOptions = FetchOptions.Builder.withLimit(MAX_BATCH);
            if (cursor != null) {
                fetchOptions.startCursor(cursor);
            }
            
            QueryResultList<Entity> results = pq.asQueryResultList(fetchOptions);
            if (results == null || results.isEmpty()) {
                break;
            }
            
            count += results.size();
            cursor = results.getCursor();            
        }
        return count;
    }
}
