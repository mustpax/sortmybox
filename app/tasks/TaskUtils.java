package tasks;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;

import common.request.Headers;

/**
 * Utilities for {@link Task}s.
 * 
 * @author syyang
 */
public class TaskUtils {
    
    private static final String TASK_PATH = "/tasks";

    public static Queue getQueue(Class<? extends Task> taskClass) {
        Preconditions.checkNotNull(taskClass, "Task class can't be null");
        return QueueFactory.getQueue(taskClass.getSimpleName());
    }
    
    public static TaskOptions newTaskOptions(Class<? extends Task> taskClass) {
        Preconditions.checkNotNull(taskClass, "Task class can't be null");
        return TaskOptions.Builder
            .withUrl(TASK_PATH)
            .header(Headers.TASK_IMPL, taskClass.getName())
            .header(Headers.TASK_NAME, taskClass.getSimpleName());
    }

    private TaskUtils() {}

}

