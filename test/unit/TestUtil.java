package unit;

import models.User;
import models.User.AccountType;

public class TestUtil {

    public static User createUser(long id) throws Exception {
        User user = new User(AccountType.DROPBOX);
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
