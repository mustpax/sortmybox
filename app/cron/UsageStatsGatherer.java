package cron;

import java.util.Date;
import java.util.Map;

import models.FileMove;
import models.UsageStats;

import org.joda.time.DateTime;

import play.Logger;
import play.modules.objectify.Datastore;

import com.google.appengine.api.users.User;
import com.google.appengine.repackaged.org.apache.commons.digester.Rule;

public class UsageStatsGatherer implements Job {

    @Override
    public void execute(Map<String, String> jobData) {
        int numUsers = Datastore.query(User.class).countAll();
        int numRules = Datastore.query(Rule.class).countAll();
        
        Date to = DateTime.now().toDateMidnight().toDate();
        Date from = DateTime.now().minusDays(1).toDateMidnight().toDate();

        int numFileMoves = Datastore.query(FileMove.class)
            .filter("when >=", from)
            .filter("when <=", to)
            .countAll();
        
        UsageStats stats = new UsageStats(numUsers, numRules, numFileMoves);
        stats.save();
        
        Logger.info("Finished collecting user stats: " + stats);
    }
}
