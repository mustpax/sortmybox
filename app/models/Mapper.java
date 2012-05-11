package models;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

/**
 * Maps {@link Entity} from/to model POJO.
 * 
 * @author syyang
 */
public interface Mapper<T> {

    Key getKey(T model);

    Entity toEntity(T model);
    
    T toModel(Entity entity);
    
    Key toKey(T model);

    Class<T> getType();
}
