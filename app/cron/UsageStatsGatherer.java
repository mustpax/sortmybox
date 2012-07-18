package cron;

import java.util.Map;

import models.DatastoreUtil;
import models.FileMove;
import models.Rule;
import models.UsageStats;
import models.User;
import play.Logger;

public class UsageStatsGatherer implements Job {

    @Override
    public void execute(Map<String, String> jobData) {
        long numUsers = DatastoreUtil.count(User.all());
        long numRules = DatastoreUtil.count(Rule.all());
        long numFileMoves = DatastoreUtil.count(FileMove.all());

        UsageStats stats = new UsageStats(numUsers, numRules, numFileMoves);
        stats.save();
        
        Logger.info("Finished collecting usage stats: " + stats);
    }

}
