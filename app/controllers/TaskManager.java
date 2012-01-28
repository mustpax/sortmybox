package controllers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;

import common.reflection.ReflectionUtils;
import common.request.Headers;

import play.mvc.Controller;
import tasks.Task;
import tasks.TaskContext;

/**
 * This class is responsible for relaying a GET request to an appropriate task.
 * The GET request is made by cron tasks or queued tasks.
 * 
 * @author syyang
 */
public class TaskManager extends Controller {
    
    private static final Logger logger = Logger.getLogger(TaskManager.class);

    /**
     * Executes the task specified by the request url.
     */
    public static void process() {
        if (isRequestFromQueueService()) {
            logger.warn("TaskManager:request is not from queue service. exiting:" + request.url);
            return;
        }
        
        Task task = null;
        TaskContext context = null;
        try {
            context = new TaskContext(request);
            task = ReflectionUtils.newInstance(Task.class, context.getTaskClassName());
        } catch (Exception e) {
            logger.error("TaskManager:failed to instantiate:" + request.url, e);
            return;
        }

        process(task, context);
    }

    private static void process(Task task, TaskContext context) {
        assert task != null && context != null: "Task and context can't be null";
        
        String taskName = context.getTaskName();
        try {
            task.execute(context);
            logger.info("TaskManager:executed successfully:" + taskName);
        } catch (Throwable t) {
            // TODO(syyang): we should send out a gack here...
            logger.error("TaskManager:" + taskName + ":failed to execute", t);
            // queued tasks rely on propagating the exception for retry
            Throwables.propagate(t);
        }
    }
    
    private static boolean isRequestFromQueueService() {
        String queueName = Headers.first(request, Headers.QUEUE_NAME);
        return queueName != null;
    }
}
