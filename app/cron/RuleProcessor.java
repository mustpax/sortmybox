package cron;

import java.util.List;
import java.util.Map;

import play.Logger;

import models.User;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.collect.Lists;

import common.util.TypedMap;

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
    /** Indicates how many users to process per task */
    public static final int DEFAULT_CHUNK_SIZE = 20;
    public static final String CHUNK_SIZE = "ChunkSize";

    @Override
    public void execute(Map<String, String> jobData) {
        TypedMap data = new TypedMap(jobData);
        final int chunkSize = data.get(CHUNK_SIZE, DEFAULT_CHUNK_SIZE);
        ChunkedRuleProcessor.submit(0, chunkSize, null);
    }
}
