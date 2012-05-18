package cron;

import java.util.Date;
import java.util.Map;

import models.DatastoreUtil;
import models.FileMove;
import models.Rule;
import models.UsageDailyStats;
import models.User;

import org.joda.time.DateTime;

import play.Logger;

import com.google.appengine.api.datastore.Query;
import com.google.common.base.Supplier;

public class UsageDailyStatsGatherer implements Job {

    private static class QuerySupplier implements Supplier<Query> {
        private final String kind;
        
        QuerySupplier(String kind) {
            this.kind = kind;
        }
        
        @Override
        public Query get() {
            return new Query(kind);
        }
    }

    @Override
    public void execute(Map<String, String> jobData) {
        Date to = DateTime.now().toDateMidnight().toDate();
        Date from = DateTime.now().minusDays(1).toDateMidnight().toDate();  
        
        long usersDelta = DatastoreUtil.count("created", from, to, new QuerySupplier(User.KIND));
        long rulesDelta = DatastoreUtil.count("created", from, to, new QuerySupplier(Rule.KIND));
        long fileMovesDelta = DatastoreUtil.count("when", from, to, new QuerySupplier(FileMove.KIND));
        
        UsageDailyStats delta = new UsageDailyStats(usersDelta, rulesDelta, fileMovesDelta);
        delta.save();
        
        Logger.info("Finished collecting daily stats: " + delta);
    }   

}
