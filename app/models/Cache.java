package models;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.Cacheable;

import play.Logger;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

class Cache {
    private static final AtomicBoolean CACHE_FIXED = new AtomicBoolean(false);

    /**
     * There's a race condition around the forcedCacheImpl being
     * copied over to cacheImpl
     */
    private static void fixCacheImpl() {
        if (CACHE_FIXED.compareAndSet(false, true)) {
            if ((play.cache.Cache.forcedCacheImpl != null) &&
                (play.cache.Cache.forcedCacheImpl != play.cache.Cache.cacheImpl)) {
                Logger.warn("Cache not initialized properly. Replacing cacheImpl %s with forcedCacheImpl %s",
			                play.cache.Cache.cacheImpl,
			                play.cache.Cache.forcedCacheImpl);
                play.cache.Cache.cacheImpl = play.cache.Cache.forcedCacheImpl;
            }
        }
    }

    private Cache() { }

    private static final Cache INSTANCE = new Cache();

    public static Cache get() {
        fixCacheImpl();
        return INSTANCE;
    }

    public boolean isCachable(Mapper<?> mapper) {
        return mapper.getType().isAnnotationPresent(Cacheable.class);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(Key k) {
        return (T) play.cache.Cache.get(KeyFactory.keyToString(k));
    }

    public <T> void put(T model, Mapper<T> mapper) {
        play.cache.Cache.set(KeyFactory.keyToString(mapper.toKey(model)), model, "1h");
    }
    
    public <T> void deleteAll(Iterable<T> models, Mapper<T> mapper) {
        for (T model: models) {
            delete(model, mapper);
        }
    }
    
    public <T> void delete(T model, Mapper<T> mapper) {
        play.cache.Cache.delete(KeyFactory.keyToString(mapper.toKey(model)));
    }
}
