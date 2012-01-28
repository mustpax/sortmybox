package cron;

import java.util.Map;

/**
 * An interface for cron jobs. Will be called by the GAE cron service.
 * 
 * @author syyang
 */
public interface Job {

    void execute(Map<String, String> jobData);

}
