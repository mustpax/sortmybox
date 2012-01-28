package unit.tasks;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;

import play.mvc.Http;
import play.mvc.Http.Header;
import tasks.Task;
import tasks.TaskContext;

import common.request.Headers;

/**
 * Unit tests for {@link TaskContext}.
 * 
 * @author syyang
 */
public class TaskContextTest {
    
    private Http.Request request;
    
    @Before
    public void setUp() {
        request = mock(Http.Request.class);
        request.headers = Maps.newHashMap();
    }
    
    @Test
    public void testConstructor() throws Exception {
        final String QUEUE_NAME = "foo";
        final String TASK_NAME = "bar";
        final int RETRY_COUNT = 3;
        final String TASK_IMPL = "foo.bar";
        
        request = mock(Http.Request.class);
        request.headers = Maps.newHashMap();
        
        put(Headers.QUEUE_NAME, QUEUE_NAME);
        put(Headers.TASK_NAME, TASK_NAME);
        put(Headers.TASK_IMPL, TASK_IMPL);
        put(Headers.TASK_RETRY_COUNT, String.valueOf(RETRY_COUNT));
        
        TaskContext context = new TaskContext(request);
        assertEquals(QUEUE_NAME, context.getQueueName());
        assertEquals(TASK_IMPL, context.getTaskClassName());
        assertEquals((Integer) RETRY_COUNT, Integer.valueOf(context.getCurrentRetryCount()));
        assertEquals(TASK_NAME, context.getTaskName());
    }
    
    private void put(String key, String value) {
        Header header = new Header();
        header.values = Arrays.asList(value);
        request.headers.put(key, header);
    }
}
