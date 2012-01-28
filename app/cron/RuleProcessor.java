package cron;

import java.util.Map;

import org.apache.log4j.Logger;

import controllers.TaskManager;

import tasks.Task;
import tasks.TaskContext;

/**
 * A scheduled task for applying rules in background.
 * 
 * @author syyang
 */
public class RuleProcessor implements Job {

    private static final Logger logger = Logger.getLogger(RuleProcessor.class);

    @Override
    public void execute(Map<String, String> jobData) {
        
    }

}
