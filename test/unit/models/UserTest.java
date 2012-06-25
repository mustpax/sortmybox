package unit.models;

import java.util.Date;

import models.User;
import models.User.AccountType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import play.cache.Cache;
import unit.TestUtil;
import dropbox.Dropbox;
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
        User user = User.findById(AccountType.DROPBOX, ID);
        if (user != null) {
            TestUtil.deleteUser(user);
        }
        Cache.clear();
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
        User user = newUser();

        assertEquals(TOKEN, user.getToken());
        user.setTokenRaw(TOKEN);
        assertEquals(TOKEN, user.getToken());
        
        assertEquals(SECRET, user.getSecret());
        user.setSecretRaw(SECRET);
        assertEquals(SECRET, user.getSecret());
    }

    @Test
    public void testSave() {
        User user = newUser();
        user.save();
        assertEquals(User.findById(AccountType.DROPBOX, ID), user);
        
        DbxAccount acc = new DbxAccount();
        acc.name = NAME;
        acc.uid = ID;
        user.sync(acc, TOKEN, SECRET);
        assertEquals(User.findById(AccountType.DROPBOX, ID), user);

        user.setToken(TOKEN + "X");
        user.save();
        assertEquals(User.findById(AccountType.DROPBOX, ID), user);
    }

    @Test
    public void testDelete() {
        User user = newUser();
        user.save();
        assertNotNull(User.findById(AccountType.DROPBOX, ID));
        user.delete();
        assertNull(User.findById(AccountType.DROPBOX, ID));
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

    @Test
    public void testCache() {
        User user = newUser();
        user.save();

        User user2 = newUser();
        // Force the Cache value to be different from datastore
        Cache.set(User.key(ID).toString(), user2);
        assertFalse(User.findById(AccountType.DROPBOX, ID).equals(user));
        assertEquals(User.findById(AccountType.DROPBOX, ID), user2);

        user.save();
        assertFalse(User.findById(AccountType.DROPBOX, ID).equals(user2));
        assertEquals(User.findById(AccountType.DROPBOX, ID), user);
    }
    
    @Test
    public void testSortingFolderUpdate() {
    	//set the sortingFolder to null for existing users
    	User user = newUser(ID, TOKEN, EMAIL, SECRET, NAME);
    	user.sortingFolder = null;
        user.save();
        //now lets get the user from the DB
        User newUser = User.findById(AccountType.DROPBOX, ID);
        //verify that the sortingFolder is set to the old value - /Sortbox
        assertEquals("Did not find expected sortingFolder for old users!",Dropbox.getOldSortboxPath(),newUser.sortingFolder);
    }

    @Test
    public void testAccountType() {
    	// Default should be Dropbox for new users
    	User user = newUser();
        user.save();
        user = User.findById(AccountType.DROPBOX, ID);
        assertSame(User.AccountType.DROPBOX, user.accountType);
        user.delete();

    	// Dropbox should be default when accountType is null
        assertAccountType(AccountType.DROPBOX, null);
        assertAccountType(AccountType.BOX, AccountType.BOX);
        assertAccountType(AccountType.DROPBOX, AccountType.DROPBOX);
    }

    private void assertAccountType(AccountType expected, AccountType set) {
    	User user = newUser();
        user.accountType = set;
        user.save();

        user = User.findById(expected, ID);
        assertSame(expected, user.accountType);

        user.delete();
    }

    public static User newUser() {
        return newUser(ID, TOKEN, EMAIL, SECRET, NAME);
    }

    public static User newUser(Long id, String token, String email, String secret, String name) {
        User user = new User(AccountType.DROPBOX);
        user.id = id;
        user.setToken(token);
        user.email = email;
        user.setSecret(secret);
        user.setName(name);
        return user;
    }
}
