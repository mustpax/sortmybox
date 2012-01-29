package func.cron;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import models.User;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.HeaderWrapper;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo.TaskStateInfo;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.common.collect.Maps;

import cron.RuleProcessor;

import play.Logger;
import play.modules.siena.SienaFixtures;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.test.BaseTest;
import tasks.ChunkedRuleProcessor;
import tasks.ChunkedRuleProcessor.ChunkInfo;
import unit.BaseTaskQueueTest;

/**
 * Functional tests for {@link RuleProcessor}.
 *
 * @author syyang
 */
public class RuleProcessorTest extends BaseTaskQueueTest {

    private static final String QUEUE_NAME = ChunkedRuleProcessor.class.getSimpleName();
    private static final int CHUNK_SIZE = 2;
    private static final int KEY_SPACE = 4;

    private LocalTaskQueue taskQueue;
    
    @BeforeClass
    public static void loadFixtures() throws Exception {
        SienaFixtures.deleteAllModels();
        SienaFixtures.loadModels("data.yml");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        this.taskQueue = LocalTaskQueueTestConfig.getLocalTaskQueue();
        
        // override the key space in ChunkedRuleProcessor
        ChunkedRuleProcessor.KEY_SPACE = KEY_SPACE;
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
        int numUsers = User.all().count();
        int queuedTasks = numUsers / CHUNK_SIZE;
        assertEquals(queuedTasks, queueInfo.getCountTasks());
        
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
        jobData.put(RuleProcessor.CHUNK_SIZE, String.valueOf(CHUNK_SIZE));
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
    
    /**
     * Tests {@link ChunkInfo}.
     */
    @Test
    public void testChunkInfo() {
        ChunkInfo info = new ChunkInfo(0, 2, 4);
        assertEquals(2, info.chunkRange);
        assertEquals(0, info.lowerBound);
        assertEquals(2, info.upperBound);
        assertFalse(info.lastChunk);
        
        info = new ChunkInfo(1, 2, 4);
        assertEquals(2, info.chunkRange);
        assertEquals(2, info.lowerBound);
        assertEquals(4, info.upperBound);
        assertTrue(info.lastChunk);
        
        try {
            new ChunkInfo(2, 2, 4);
            fail("chunk index should be less than the number of chunks");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
}
