package func.cron;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import models.User;

import org.junit.BeforeClass;
import org.junit.Test;

import play.Logger;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import rules.ChunkedRuleProcessor;
import rules.RuleProcessor;
import unit.BaseTaskQueueTest;
import unit.TestUtil;

import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.HeaderWrapper;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.TaskStateInfo;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.collect.Maps;


/**
 * Functional tests for {@link RuleProcessor}.
 *
 * @author syyang
 */
public class RuleProcessorTest extends BaseTaskQueueTest {

    private static final String QUEUE_NAME = ChunkedRuleProcessor.class.getSimpleName();
    private static final int CHUNK_SIZE = 2;

    private LocalTaskQueue taskQueue;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        this.taskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
    
        // create 4 users
        int id = 100;
        for (int i = 0; i < 4; i++) {
            TestUtil.createUser(id);
            id += 100;
        }
    }
    
    @Override
    public void tearDown() throws Exception {
        try {
            taskQueue.flushQueue(QUEUE_NAME);
        } finally {
            super.tearDown();
        }
    }

    /**
     * End to end test for {@link RuleProcessor}.
     */
    @Test
    public void testEnd2End() throws Exception {
        // 1. Run the RuleProcessor job once.
        runRuleProcessor();
        
        // 2. Verify the number of enqueued tasks.
        QueueStateInfo queueInfo = getQueueStateInfo();
        assertEquals(2, queueInfo.getCountTasks());
        
        // 3. Manually processes the tasks
        for (TaskStateInfo taskInfo : queueInfo.getTaskInfo()) {
            Response response = executeTask(taskInfo);
            assertStatus(200, response);
            // TODO(syyang): verify the rules are actually applied
        }
    }
    
    private QueueStateInfo getQueueStateInfo() {
        Map<String, QueueStateInfo> allQueues = taskQueue.getQueueStateInfo();
        return allQueues.get(QUEUE_NAME);
    }
    
    private static void runRuleProcessor() {
        Map<String, String> jobData = Maps.newHashMap();
        jobData.put(RuleProcessor.CHUNK_SIZE, Integer.toString(CHUNK_SIZE));
        new RuleProcessor().execute(jobData);
    }

    private static Response executeTask(TaskStateInfo taskInfo) {
        List<HeaderWrapper> wrappers = taskInfo.getHeaders();        
        Request request = newRequest();
        
        // generate request headers
        request.headers = Maps.newHashMap();
        for (HeaderWrapper wrapper : wrappers) {
            Header header = new Header();
            header.values = Arrays.asList(wrapper.getValue());
            request.headers.put(wrapper.getKey(), header);
        }
        String queuedTask = taskInfo.getBody();
        return POST(request, "/tasks", APPLICATION_X_WWW_FORM_URLENCODED, queuedTask);
    }
}
