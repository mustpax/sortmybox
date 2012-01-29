package unit;


import java.io.File;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import play.test.FunctionalTest;
import play.test.UnitTest;

import com.google.appengine.api.taskqueue.dev.LocalTaskQueue;
import com.google.appengine.api.taskqueue.dev.QueueStateInfo;
import com.google.appengine.tools.development.LocalServerEnvironment;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

import junit.framework.TestCase;

/**
 * Base test for testing task queue. Ripped off from:
 * <p>
 * http://turbomanage.wordpress.com/2010/03/03/a-recipe-for-unit-testing-appengine-task-queues/
 * 
 */
public abstract class BaseTaskQueueTest extends FunctionalTest {

    protected LocalServiceTestHelper helper;
    private LocalDatastoreServiceTestConfig datastoreConfig;
    private LocalTaskQueueTestConfig taskQueueConfig;
    
    @Before
    public void setUp() throws Exception {
        datastoreConfig = new LocalDatastoreServiceTestConfig();
        taskQueueConfig = new LocalTaskQueueTestConfig();
        helper = new LocalServiceTestHelper(datastoreConfig, taskQueueConfig) {
            @Override
            protected LocalServerEnvironment newLocalServerEnvironment() {
                final LocalServerEnvironment lse = super.newLocalServerEnvironment();
                    return new LocalServerEnvironment() {
                        @Override public File getAppDir() { return new File("war"); }
                        @Override public String getAddress() { return lse.getAddress(); }
                        @Override public int getPort() { return lse.getPort(); }
                        @Override public boolean enforceApiDeadlines() { return false; }
                        @Override public String getHostName() { return null; }
                        @Override public boolean simulateProductionLatencies() { return false; }
                        @Override public void waitForServerToStart() throws InterruptedException {
                            lse.waitForServerToStart();
                        }
                    };
            }
        };
        helper.setEnvAuthDomain("auth");
        helper.setEnvEmail("test@example.com");
        helper.setEnvIsAdmin(true);
        helper.setEnvIsLoggedIn(true);
        helper.setUp();
    }
    
    @After
    public void tearDown() throws Exception {
        helper.tearDown();
    }
}
