package unit;

import models.User;
import play.modules.objectify.Datastore;

public class TestUtil {

    public static User createUser(long id) throws Exception {
        User user = new User();
        user.id = id;
        return createUser(user);
    }

    public static User createUser(User user) throws Exception {
        Datastore.put(user);
        return user;
    }

    public static void deleteUser(User user) throws Exception {
        Datastore.delete(user);
    }
}
