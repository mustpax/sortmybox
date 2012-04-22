package unit;

import models.User;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

public class TestUtil {

    public static User createUser(long id) throws Exception {
        User user = new User();
        user.id = id;
        return createUser(user);
    }

    public static User createUser(User user) throws Exception {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService(); 
        Entity entity = user.toEntity();
        ds.put(entity);
        return new User(ds.get(entity.getKey()));
    }

    public static void deleteUser(User user) throws Exception {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Entity entity = user.toEntity();
        ds.delete(entity.getKey());
    }
}
