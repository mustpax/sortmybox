package common.request;

import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;

/**
 * Utilities for interacting with {@link Http.Request.Header}.
 * 
 * Note that Play framework lower-cases the header keys.
 * 
 * @author syyang
 */
public class Headers {

    /** Requests from the cron service */
    public static final String CRON = "x-appengine-cron";
    
    /** Requests from the task queue service */
    public static final String QUEUE_NAME = "x-appengine-queuename";
    public static final String TASK_ID = "x-appengine-taskname";
    public static final String TASK_RETRY_COUNT = "x-appengine-taskretrycount";
    public static final String FAIL_FAST = "x-appengine-failfast";

    /** Sortbox headers */
    public static final String TASK_IMPL = "x-sortbox-taskimpl";
    public static final String TASK_NAME = "x-sortbox-taskname";

    public static final String FORWARDED_FOR = "x-forwarded-for";
    public static final String FORWARDED_PROTO = "x-forwarded-proto";
    
    
    /**
     * Returns the first header value for the specified key, null if no
     * header is found for the key
     * 
     * @param request The request object for the current request context
     * @param key The header key to look up
     * @return The header value, null if not found
     */
    public static String first(Http.Request request, Object key) {
        return first(request, key, true);
    }
    
    /**
     * Returns the first header value for the specified key.
     * 
     * @param request The request object for the current request context
     * @param key The header key to look up
     * @param allowNull Whether to fail if the header value is null
     * @return The header value, null if the header value is null and allowNull is true
     * @throws NullPointerException Thrown when the header value is null and allowNull is false
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
