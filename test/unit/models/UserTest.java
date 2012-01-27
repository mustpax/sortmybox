package unit.models;

import models.User;

import org.junit.After;
import org.junit.Test;

import dropbox.gson.DbxUser;

import play.test.UnitTest;

/**
 * Unit tests for {@link User}.
 * 
 * @author syyang
 */
public class UserTest extends UnitTest {

    private static final long ID = 1L;
    private static final String TOKEN = "abc";
    private static final String SECRET = "def";
    private static final String EMAIL = "foo@bar";
    private static final String NAME = "john doe";

    @After
    public void tearDown() {
        User user = User.findById(ID);
        if (user != null) {
            user.delete();
        }
    }

    @Test
    public void testCRUD() throws Exception {
        User user = new User(ID, TOKEN, EMAIL, SECRET, NAME);
        assertNull(User.findById(ID));

        // verify insert
        user.insert();
        assertEquals(user, User.findById(ID));

        // verify update
        String secret2 = SECRET + "2";
        user.secret = secret2;
        user.update();
        assertEquals(user, User.findById(ID));

        // verify delete
        user.delete();
        assertNull(User.findById(ID));
    }

    @Test
    public void testFindOrCreateByDbxUser() throws Exception {
        // if DbxUser is null, findOrCreateByDbxUser should just return null
        assertNull(User.findOrCreateByDbxUser(null, TOKEN, SECRET));

        DbxUser dbxUser = new DbxUser();
        dbxUser.uid = ID;
        dbxUser.name = NAME;

        // verify a new User is created
        User user = new User(dbxUser, TOKEN, SECRET);
        assertEquals(user, User.findOrCreateByDbxUser(dbxUser, TOKEN, SECRET));

        // verify stale fields are updated
        String token2 = TOKEN + "2";
        String secret2 = SECRET + "2";
        user.token = token2;
        user.secret = secret2;
        assertEquals(user, User.findOrCreateByDbxUser(dbxUser, token2, secret2));
    }
}
