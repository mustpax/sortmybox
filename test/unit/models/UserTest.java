package unit.models;

import models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import unit.TestUtil;
import dropbox.gson.DbxAccount;

/**
 * Unit tests for {@link User}.
 * 
 * @author syyang
 */
public class UserTest extends BaseModelTest {

    private static final long ID = 67676767L;
    private static final String TOKEN = "abcd";
    private static final String SECRET = "defg";
    private static final String EMAIL = "foo@bar";
    private static final String NAME = "john doe";

    @Before
    @After
    public void clean() throws Exception {
        User user = User.findById(ID);
        if (user != null) {
            TestUtil.deleteUser(user);
        }
    }

    @Test
    public void testFindOrCreateByDbxAccount() throws Exception {
        // if DbxAccount is null, getOrCreateUser should just return null
        assertNull(User.getOrCreateUser(null, TOKEN, SECRET));

        DbxAccount account = new DbxAccount();
        account.uid = ID;
        account.name = NAME;

        assertNotNull(User.getOrCreateUser(account, TOKEN, SECRET).id);
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

        User u = User.getOrCreateUser(account, TOKEN, SECRET);
        assertNotNull(u.modified);
        assertNotNull(u.created);
        assertFalse(u.modified.before(u.created));
        
        // Updating token and secret should update modification date but not creation date
        u = User.getOrCreateUser(account, TOKEN + "x", SECRET + "x");
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
