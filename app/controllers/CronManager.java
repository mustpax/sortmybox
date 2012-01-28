package controllers;

import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import common.reflection.ReflectionUtils;
import common.request.Headers;
import cron.Job;

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

    private static final Logger logger = Logger.getLogger(CronManager.class);

    public static final String pJOB_NAME = "jobName";
    
    public static void process() {
        if (isRequestFromCronService()) {
            logger.warn("CronManager:request is not from cron service. exiting:" + request.url);
            return;
        }

        Job job = null;
        try {
            String className = getJobClassName(request.url);
            job = ReflectionUtils.newInstance(Job.class, className);
        } catch (Exception e) {
            logger.error("CronManager:failed to instantiate:"+request.url, e);
            return;
        }
        
        process2(job, request.params.allSimple());
    }

    private static void process2(Job job, Map<String, String> jobData) {
        assert job != null : "Job can't be null";
        assert jobData != null : "Job data can't be null";
        
        String jobName = job.getClass().getSimpleName();
        try {
            job.execute(jobData);
            logger.info("CronManager:executed successfully:" + jobName);
        } catch (Throwable t) {
            // TODO (syyang): we should send out a gack here...
            logger.error("CronManager:" + jobName + ":failed to execute", t);
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
