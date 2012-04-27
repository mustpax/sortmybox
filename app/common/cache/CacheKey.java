package common.cache;

import com.google.appengine.repackaged.com.google.common.base.Joiner;

public class CacheKey {

    private static final Joiner JOINER = Joiner.on(":");
    
    private CacheKey() {}
    
    public static String create(Class<?> clazz, Long id) {
        return JOINER.join(clazz.getSimpleName(), id);
    }

}
