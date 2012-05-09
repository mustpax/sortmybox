package models;

import com.google.appengine.api.datastore.Entity;

/**
 * Maps {@link Entity} from/to model POJO.
 * 
 * @author syyang
 */
public interface Mapper<T> {

    Entity toEntity(T model);
    
    T toModel(Entity entity);

}
