package models;

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
    
    private DatastoreUtil() {}
    
    public static Iterable<Key> extractKeys(Iterable<Entity> entities) {
        return Iterables.transform(entities, TO_KEY);
    }
  
    public static Key newKey(Class<?> clazz, long id) {
        return KeyFactory.createKey(clazz.getSimpleName(), id);
    }
    
    public static Entity newEntity(Class<?> clazz, Long id) {
        if (id == null) {
            return new Entity(clazz.getSimpleName());
        } else {
            return new Entity(newKey(clazz, id));
        }
    }
    
    public static <T> T get(Key key, Mapper<T> mapper) {
        try {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            Entity entity = ds.get(key);
            return mapper.toModel(entity);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public static <T> void put(List<T> models, Mapper<T> mapper) {
        List<Entity> entities = Lists.newArrayListWithCapacity(models.size());
        for (T model : models) {
            entities.add(mapper.toEntity(model));
        }
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        ds.put(entities);
    }
    
    public static <T> List<T> asList(Query q, FetchOptions options, Mapper<T> mapper) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = ds.prepare(q);
        List<Entity> entities = pq.asList(options);
        List<T> models = Lists.newArrayListWithCapacity(entities.size());
        for (Entity entity : entities) {
            models.add(mapper.toModel(entity));
        }
        return models;
    }
}
