package unit;

import models.User;

public class TestUtil {

    public static User createUser(long id) throws Exception {
        User user = new User();
        user.id = id;
        user.save();
        return user;
    }

    public static User createUser(User user) throws Exception {
        user.save();
        return user;
    }

    public static void deleteUser(User user) throws Exception {
        user.delete();
    }
}
