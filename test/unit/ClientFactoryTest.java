package unit;

import models.User;
import models.User.AccountType;

import org.junit.Test;

import box.BoxClientFactory;

import dropbox.client.DropboxClientFactory;

import play.test.UnitTest;

public class ClientFactoryTest extends UnitTest {
    private User getUser() {
        User u = new User(AccountType.DROPBOX);
        u.setSecret("x");
        u.setToken("x");
        return u;
    }
    
    @Test
    public void testBoxClientFactory() {
        User u = getUser();
        u.accountType = AccountType.BOX;
        assertNotNull(BoxClientFactory.create(u));
        
        try {
            u.accountType = AccountType.DROPBOX;
            BoxClientFactory.create(u);
            fail();
        } catch (AssertionError expected) {
            // should throw exception
        }
    }

    @Test
    public void testDropboxClientFactory() {
        User u = getUser();
        u.accountType = AccountType.DROPBOX;
        assertNotNull(DropboxClientFactory.create(u));
        
        try {
            u.accountType = AccountType.BOX;
            DropboxClientFactory.create(u);
            fail();
        } catch (AssertionError expected) {
            // should throw exception
        }
    }
}
