package controllers;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import common.reflection.ReflectionUtils;
import common.request.Headers;
import cron.Job;

import play.Logger;
import play.mvc.Controller;
import tasks.TaskContext;

/**
 * This class is responsible for relaying a GET request to an appropriate {@link Job}.
 * The GET request is made by cron service.
 * <p>
 * The job to run is determined from the request url. For example, the request url 
 * <code>/cron/foo/bar</code> will be interpreted as to run <code>cron.fool.bar</code>.
 * 
 * @author syyang
 */
public class CronManager extends Controller {

    public static final String pJOB_NAME = "jobName";
    
    public static void process() {
        if (isRequestFromCronService()) {
            Logger.warn("CronManager: request is not from cron service: " + request.url);
            return;
        }

        Job job = null;
        try {
            String className = getJobClassName(request.url);
            job = ReflectionUtils.newInstance(Job.class, className);
        } catch (Exception e) {
            Logger.error("CronManager failed to instantiate: "+request.url, e);
            return;
        }
        
        process(job, request.params.allSimple());
    }

    private static void process(Job job, Map<String, String> jobData) {
        assert job != null : "Job can't be null";
        assert jobData != null : "Job data can't be null";
        
        String jobName = job.getClass().getSimpleName();
        try {
            job.execute(jobData);
            Logger.info("CronManager successfully executed: " + jobName);
        } catch (Throwable t) {
            // TODO (syyang): we should send out a gack here...
            Logger.error("CronManager failed to execute: " + jobName, t);
            // queued tasks rely on propagating the exception for retry
            Throwables.propagate(t);
        }
    }

    /**
     * Gets the full job class name from the request url.
     */
    public static String getJobClassName(String requestUrl) {
        Preconditions.checkNotNull(requestUrl, "request url can't be null");
        // skip the first "/" and replace the remaining "/" with "."
        // e.g. /cron/foo/bar will be transformed to cron.foo.bar
        return requestUrl.substring(1).replace('/', '.');
    }
    
    private static boolean isRequestFromCronService() {
        String isCron = Headers.first(request, Headers.CRON);
        return "true".equals(isCron);
    }
}
