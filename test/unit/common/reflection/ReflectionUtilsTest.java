package unit.common.reflection;

import org.junit.Test;

import play.test.UnitTest;
import tasks.Task;
import tasks.TaskContext;

import common.reflection.ReflectionUtils;

/**
 * Unit tests for {@link ReflectionUtils} 
 */
public class ReflectionUtilsTest extends UnitTest {

    public static class DummyTask implements Task {
        @Override
        public void execute(TaskContext context) throws Exception {}
    }
    
    @Test
    public void testNewInstance_negative() throws Exception {
        try {
            ReflectionUtils.newInstance(Task.class, "Foo");
            fail();
        } catch (Exception e) {
            // expected
        }
        
        Task task = ReflectionUtils.newInstance(Task.class, DummyTask.class.getName());
        assertTrue("unexpected Task implementation", task instanceof DummyTask);
    }
    
}
