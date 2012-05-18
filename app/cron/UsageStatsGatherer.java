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
