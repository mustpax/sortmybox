package models;

import java.util.Collections;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Persistence utils.
 * 
 * @author syyang
 */
public class DatastoreUtil {

    public static final Function<Entity, Key> TO_KEY = new Function<Entity, Key>() {
        @Override public Key apply(Entity entity) {
            return entity.getKey();
        }
    };
    
    public static class ToEntityFunction<T> implements Function<T, Entity> {
        private final Mapper<T> m;
        public ToEntityFunction(Mapper<T> m) {
            this.m = m;
        }

        @Override
        public Entity apply(T t) {
            return m.toEntity(t);
        }
    }
    
    public static class FromEntityFunction<T> implements Function<Entity, T> {
        private final Mapper<T> m;

        public FromEntityFunction(Mapper<T> m) {
            this.m = m;
        }

        @Override
        public T apply(Entity e) {
            return m.toModel(e);
        }
    }
    
    public static class ModelToKeyFunction<T> implements Function<T, Key> {
        private final Mapper<T> m;

        public ModelToKeyFunction(Mapper<T> m) {
            this.m = m;
        }

        @Override
        public Key apply(T t) {
            return m.toKey(t);
        }
    }

    private DatastoreUtil() {}
    
    public static Iterable<Key> extractKeys(Iterable<Entity> entities) {
        return Iterables.transform(entities, TO_KEY);
    }
    
    public static <T> T get(Key key, Mapper<T> mapper) {
        try {
            T ret = null;
            if (Cache.get().isCachable(mapper)) {
                ret = Cache.get().get(key);
            }
            
            if (ret == null) {
                DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
                Entity entity = ds.get(key);
                ret = mapper.toModel(entity);
                if (Cache.get().isCachable(mapper)) {
                    Cache.get().put(ret, mapper);
                }
            }
            return ret;
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public static <T> Key put(T model, Mapper<T> mapper) {
        return put(Collections.singleton(model), mapper).get(0);
    }

    public static <T> List<Key> put(Iterable<T> models, Mapper<T> mapper) {
        ToEntityFunction<T> func = new ToEntityFunction<T>(mapper);
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        if (Cache.get().isCachable(mapper)) {
            Cache.get().deleteAll(models, mapper);
        }
        return ds.put(Iterables.transform(models, func));
    }
    
    public static <T> void delete(List<T> models, final Mapper<T> mapper) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Cache.get().deleteAll(models, mapper);
        ds.delete(Lists.transform(models, new ModelToKeyFunction<T>(mapper)));
    }

    public static <T> Iterable<T> query(Query q, FetchOptions options, Mapper<T> mapper) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(q);
        return Iterables.transform(pq.asIterable(), new FromEntityFunction<T>(mapper));
    }

    public static <T> Iterable<Key> queryKeys(Query q, FetchOptions options, Mapper<T> mapper) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(q.setKeysOnly());
        return Iterables.transform(pq.asIterable(), TO_KEY);
    }

    public static <T> List<T> asList(Query q, FetchOptions options, Mapper<T> mapper) {
        return Lists.newArrayList(query(q, options, mapper));
    }

    public static int count(Query q) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(q.setKeysOnly());
        return pq.countEntities(FetchOptions.Builder.withDefaults());
    }

    public static <T> void delete(T model, Mapper<T> mapper) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        if (Cache.get().isCachable(mapper)) {
            Cache.get().delete(model, mapper);
        }
        ds.delete(mapper.toKey(model));
    }
}
