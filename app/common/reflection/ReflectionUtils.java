package common.reflection;

import java.lang.reflect.Constructor;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Reflection utilities
 *
 * @author syyang
 */
public class ReflectionUtils {

    private static final Class<?>[] EMPTY_ARRAY = new Class[]{};
    private static final Map<String, Constructor<?>> CONSTRUCTOR_CACHE =
        Maps.newConcurrentMap();
    
    /**
     * Creates a new zero-arg instance of the specified class
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> theClass, String className) {
        try {
            Constructor<T> ctr = (Constructor<T>) CONSTRUCTOR_CACHE.get(className);
            if (ctr == null) {

                Class<T> clazz = (Class<T>) Class.forName(className);
                ctr = (Constructor<T>) clazz.getDeclaredConstructor(EMPTY_ARRAY);
                ctr.setAccessible(true);
                CONSTRUCTOR_CACHE.put(className, ctr);
            }
            return ctr.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate task: " + className, e);
        }
    }

}
