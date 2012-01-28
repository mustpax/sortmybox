package common.request;

import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;

/**
 * Utilities for {@link Http.Request.Header}.
 * 
 * @author syyang
 */
public class Headers {

    /** Requests from the cron service */
    public static final String CRON = "X-AppEngine-Cron";
    
    /** Requests from the task queue service */
    public static final String QUEUE_NAME = "X-AppEngine-QueueName";
    public static final String TASK_RETRY_COUNT = "X-AppEngine-TaskRetryCount";
    public static final String FAIL_FAST = "X-AppEngine-FailFast";

    /** Sortbox headers */
    public static final String TASK_IMPL = "X-SortBox-TaskImpl";
    public static final String TASK_NAME = "X-SortBox-TaskName";

    
    public static String first(Http.Request request, Object key) {
        return first(request, key, true);
    }
    
    /**
     * Returns the first header value for the specified key, null
     * if no header is found for the key
     * 
     * @param request The request object for the current request context
     * @param key The header key to look up
     * @param allowNull Whether to fail if the header value is null
     * @return the header value, null if not found
     */
    public static String first(Http.Request request, Object key, boolean allowNull) {
        String value = null;
        if (request != null) {
            if (request.headers.containsKey(key)) {
                Header header = request.headers.get(key);
                value = header.value();
            }
        }
        if (!allowNull && value == null) {
            throw new NullPointerException("Header value not found for " + key);
        }
        return value;
    }
    
    private Headers() {}
}
