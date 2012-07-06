package unit;

import models.User;
import models.User.AccountType;

public class TestUtil {

    public static User createUser(long id) throws Exception {
        AccountType type = (id % 2) == 0 ? AccountType.DROPBOX : AccountType.BOX;
        User user = new User(type);
        user.id = id;
        user.setName("Name " + id);
        user.setToken("sometoken");
        if (type == AccountType.DROPBOX) {
            user.setSecret("somesecret");
        } else if (type == AccountType.BOX) {
            user.email = "e@mail.com";
        }
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
