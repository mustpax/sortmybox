package unit.models;

import java.util.Date;

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
        assertNull(User.upsert(null, TOKEN, SECRET));

        DbxAccount account = new DbxAccount();
        account.uid = ID;
        account.name = NAME;

        assertNotNull(User.upsert(account, TOKEN, SECRET).id);
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
    public void testSave() {
        User user = newUser(ID, TOKEN, EMAIL, SECRET, NAME);
        user.save();
        User user2 = User.findById(ID);
        assertNotSame(user2, user);
        assertEquals(user2, user);
    }

    @Test
    public void testDelete() {
        User user = newUser(ID, TOKEN, EMAIL, SECRET, NAME);
        user.save();
        assertNotNull(User.findById(ID));
        user.delete();
        assertNull(User.findById(ID));
    }

    @Test
    public void testModstamp() {
        DbxAccount account = new DbxAccount();
        account.uid = ID;
        account.name = NAME;

        User u = User.upsert(account, TOKEN, SECRET);
        assertNotNull(u.modified);
        assertNotNull(u.created);
        assertTrue(u.modified.equals(u.created) || u.modified.after(u.created));
        Date mod1 = u.modified;
        
        // Updating token and secret should update modification date but not creation date
        u = User.upsert(account, TOKEN + "x", SECRET + "x");
        assertNotNull(u.modified);
        assertNotNull(u.created);
        assertTrue(u.modified.after(u.created));
        assertTrue(u.modified.after(mod1));
    }

    private static User newUser(Long id, String token, String email, String secret, String name) {
        User user = new User();
        user.id = id;
        user.setToken(token);
        user.email = email;
        user.setSecret(secret);
        user.setName(name);
        return user;
    }
}
