package cron;

import java.util.Date;
import java.util.Map;

import models.DailyUsageStats;
import models.DatastoreUtil;
import models.FileMove;
import models.QuerySupplier;
import models.Rule;
import models.User;

import org.joda.time.DateTime;

import play.Logger;

/**
 * Background process that gathers daily (delta) usage stats.
 * 
 * @author syyang
 */
public class DailyUsageStatsGatherer implements Job {

    @Override
    public void execute(Map<String, String> jobData) {
        Date to = DateTime.now().toDateMidnight().toDate();
        Date from = DateTime.now().minusDays(1).toDateMidnight().toDate();  

        long usersDelta = DatastoreUtil.count("created", from, to, new QuerySupplier(User.KIND));
        long rulesDelta = DatastoreUtil.count("created", from, to, new QuerySupplier(Rule.KIND));
        long fileMovesDelta = DatastoreUtil.count("when", from, to, new QuerySupplier(FileMove.KIND));
        
        DailyUsageStats delta = new DailyUsageStats(usersDelta, rulesDelta, fileMovesDelta);
        delta.save();
        
        Logger.info("Finished collecting daily stats: " + delta);
    }   

}
