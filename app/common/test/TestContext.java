package common.test;

import play.Play;

/**
 * Provides contexts for tests.
 * 
 * @author syyang
 */
public class TestContext {

    private static class Holder {
        static final TestContext INSTANCE = new TestContext();
        
        static TestContext get() {
            return Holder.INSTANCE;
        }
    }
    
    private final boolean isRunningTest;
    
    private TestContext() {
        if (Play.id != null && "test".equals(Play.id)) {
            isRunningTest = true;
        } else {
            isRunningTest = false;
        }
    }

    /**
     * @return whether the app is running in tests.
     */
    public static boolean isRunningTest() {
        return Holder.get().isRunningTest;
    }

}
