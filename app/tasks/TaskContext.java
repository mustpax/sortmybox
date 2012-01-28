package tasks;

import java.util.Map;

import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import common.request.Headers;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;

/**
 * A container for task context parameters.
 * 
 * @author syyang
 */
public class TaskContext {

    private final String queueName;
    private final String taskName;
    private final int currentRetryCount;
    private final String taskClassName;
    private final Map<String, String> params;
    
    public TaskContext(Http.Request request) {
        Preconditions.checkNotNull(request, "Http request can't be null");
        // per http://code.google.com/appengine/docs/java/taskqueue/overview-push.html
        this.queueName = Headers.first(request, Headers.QUEUE_NAME, false);
        this.taskName = Headers.first(request, Headers.TASK_NAME, false);
        String currentRetryCount = Headers.first(request, Headers.TASK_RETRY_COUNT, false);
        this.currentRetryCount = Integer.valueOf(currentRetryCount);
        
        this.taskClassName = Headers.first(request, Headers.TASK_IMPL);
        this.params = ImmutableMap.copyOf(request.params.allSimple());  
    }
    
    public TaskContext(String queueName,
                       String taskName,
                       int currentRetryCount,
                       String taskClassName,
                       Map<String, String> params) {
        this.queueName = queueName;
        this.taskName = taskName;
        this.currentRetryCount = currentRetryCount;
        this.taskClassName = taskClassName;
        this.params = params;
    }

    public String getQueueName() {
        return queueName;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public int getCurrentRetryCount() {
        return currentRetryCount;
    }

    public String getTaskClassName() {
        return taskClassName;
    }
    
    public String getParam(String key) {
        return params.get(key);
    }
}
