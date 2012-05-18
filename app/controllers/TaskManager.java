package controllers;

import play.Logger;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import tasks.Task;
import tasks.TaskContext;

import com.google.common.base.Throwables;
import common.reflection.ReflectionUtils;
import common.request.Headers;

/**
 * This class is responsible for relaying a GET request to an appropriate task.
 * The GET request is made by cron tasks or queued tasks.
 * 
 * @author syyang
 */
@With({ ErrorReporter.class, Namespaced.class })
public class TaskManager extends Controller {

    /**
     * Executes the task specified by the request url.
     */
    public static void process() {
        Task task = null;
        TaskContext context = null;
        try {
            context = new TaskContext(request);
            task = ReflectionUtils.newInstance(Task.class, context.getTaskClassName());
        } catch (Exception e) {
            Logger.error("TaskManager failed to instantiate: " + request.url, e);
            error(e);
        }

        process(task, context);
    }

    private static void process(Task task, TaskContext context) {
        assert task != null && context != null: "Task and context can't be null";
        
        String taskName = context.getTaskName();
        try {
            task.execute(context);
            Logger.info("TaskManager succesfully executed: " + taskName);
        } catch (Throwable t) {
            Logger.error("TaskManager:" + taskName + ":failed to execute", t);
            // queued tasks rely on propagating the exception for retry
            Throwables.propagate(t);
        }
    }

    static void checkPermission() {
        if (! Play.mode.isProd()) {
            return;
        }
        if (isRequestFromQueueService()) {
            return;
        }
        if (Login.isAdmin()) {
            return;
        }

        Logger.warn("TaskManager:request is not from queue service.");
        forbidden();
    }

    private static boolean isRequestFromQueueService() {
        String queueName = Headers.first(request, Headers.QUEUE_NAME);
        return queueName != null;
    }
}
