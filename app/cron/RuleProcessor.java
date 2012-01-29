package cron;

import java.util.List;
import java.util.Map;

import play.Logger;

import models.User;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;

import controllers.TaskManager;

import siena.Query;
import tasks.ChunkedRuleProcessor;
import tasks.Task;
import tasks.TaskContext;
import tasks.TaskUtils;

/**
 * A scheduled task for applying rules in background.
 * 
 * @author syyang
 */
public class RuleProcessor implements Job {

    private int chunkSize = 2;

    @Override
    public void execute(Map<String, String> jobData) {
        Queue queue = TaskUtils.getQueue(ChunkedRuleProcessor.class);
        
        // TODO(syyang): do chunking
        List<String> taskIds = Lists.newArrayList();
        for (int i = 0; i < chunkSize; i++) {
            TaskOptions options = TaskUtils.newTaskOptions(ChunkedRuleProcessor.class);
            TaskHandle handle = queue.add(options);
            taskIds.add(handle.getName());
        }
        Logger.info("RuleProcessor finished enqueuing chunked tasks:" + taskIds);
    }

}
