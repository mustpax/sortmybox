package func.cron;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.junit.Test;

import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import rules.ChunkedRuleProcessor;
import rules.RuleProcessor;
import unit.BaseTaskQueueTest;
import unit.TestUtil;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.HeaderWrapper;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.TaskStateInfo;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;


/**
 * Functional tests for {@link RuleProcessor}.
 *
 * @author syyang
 */
public class RuleProcessorTest extends BaseTaskQueueTest {

    private static final int USERS = 31;
    private static final String QUEUE_NAME = ChunkedRuleProcessor.class.getSimpleName();
    private static final int CHUNK_SIZE = 3;

    private LocalTaskQueue taskQueue;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        this.taskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
    
        // create 4 users
        int id = 100;
        for (int i = 0; i < USERS; i++) {
            TestUtil.createUser(id);
            id += 5;
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
        int chunks = USERS / CHUNK_SIZE;
        if ((USERS % CHUNK_SIZE) > 0) {
            chunks++;
        }
        assertEquals(chunks, queueInfo.getCountTasks());

        // 3. Manually processes the tasks
        List<TaskStateInfo> states = queueInfo.getTaskInfo();
        for (int i = 0; i < states.size(); i++) {
            TaskStateInfo taskInfo = states.get(i);
            Response response = executeTask(taskInfo);
            assertStatus(200, response);
            Map<String, List<String>> params = new QueryStringDecoder("http://dummy/a?" + taskInfo.getBody())
                .getParameters();
            Key first = KeyFactory.stringToKey(params.get(ChunkedRuleProcessor.pSTART_ID)
                                                     .get(0));
            Key last = KeyFactory.stringToKey(params.get(ChunkedRuleProcessor.pLAST_ID)
                                                     .get(0));

            int size = Iterables.size(ChunkedRuleProcessor.getUsersForKeyRange(first, last));
            // Last chunk should contain leftover items, every other chunk should be full size.
            int expected = ((i + 1) == states.size()) ? (USERS % CHUNK_SIZE) :
                                                        CHUNK_SIZE;
            assertEquals(String.format("Bad size. First key: %s Last key: %s", first, last),
                         expected, size);
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
