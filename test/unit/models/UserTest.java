package unit.models;

import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.test.UnitTest;
import dropbox.gson.DbxAccount;

/**
 * Unit tests for {@link User}.
 * 
 * @author syyang
 */
public class UserTest extends UnitTest {

    private static final long ID = 67676767L;
    private static final String TOKEN = "abcd";
    private static final String SECRET = "defg";
    private static final String EMAIL = "foo@bar";
    private static final String NAME = "john doe";

    @Before
    @After
    public void clean() {
        User user = User.findById(ID);
        if (user != null) {
            user.delete();
        }
    }

    @Test
    public void testCRUD() throws Exception {
        User user = newUser(ID, TOKEN, EMAIL, SECRET, NAME);
        assertNull(User.findById(ID));

        // verify insert
        user.insert();
        assertEquals(user, User.findById(ID));

        // verify update
        String secret2 = SECRET + "2";
        user.setSecret(secret2);
        user.update();
        assertEquals(user, User.findById(ID));

        // verify delete
        user.delete();
        assertNull(User.findById(ID));
    }

    @Test
    public void testFindOrCreateByDbxAccount() throws Exception {
        // if DbxAccount is null, findOrCreateByDbxAccount should just return null
        assertNull(User.findOrCreateByDbxAccount(null, TOKEN, SECRET));

        DbxAccount account = new DbxAccount();
        account.uid = ID;
        account.name = NAME;

        assertNotNull(User.findOrCreateByDbxAccount(account, TOKEN, SECRET).id);
    }
    
    /**
     * Verify that secret and token fields are encrypted if they are
     */
    @Test
    public void testEncryptUnencrypted() {
        User user = newUser(ID, TOKEN, EMAIL, SECRET, NAME);

        assertEquals(TOKEN, user.getToken());
        user.setTokenRaw(TOKEN);
        assertEquals(TOKEN, user.getToken());
        
        assertEquals(SECRET, user.getSecret());
        user.setSecretRaw(SECRET);
        assertEquals(SECRET, user.getSecret());
    }
    
    @Test
    public void testModstamp() {
        DbxAccount account = new DbxAccount();
        account.uid = ID;
        account.name = NAME;

        User u = User.findOrCreateByDbxAccount(account, TOKEN, SECRET);
        assertNotNull(u.modified);
        assertNotNull(u.created);
        assertEquals(u.modified, u.created);
        
        // Updating token and secret should update modification date but not creation date
        u = User.findOrCreateByDbxAccount(account, TOKEN + "x", SECRET + "x");
        assertNotNull(u.modified);
        assertNotNull(u.created);
        assertTrue(u.modified.after(u.created));
    }

    private static User newUser(Long id, String token, String email, String secret, String name) {
        User user = new User();
        user.id = id;
        user.setToken(token);
        user.email = email;
        user.setSecret(secret);
        user.name = name;
        return user;
    }
}
