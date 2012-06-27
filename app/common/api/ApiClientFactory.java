package common.api;

import models.User;
import box.BoxClientFactory;
import dropbox.client.DropboxClientFactory;

/**
 * Singleton class for generating generating ApiClient for
 * users.
 * 
 * @author paksoy
 */
public class ApiClientFactory {
    public static ApiClient create(User u) {
        switch (u.accountType) {
        case BOX:
            return BoxClientFactory.create(u);
        case DROPBOX:
            return DropboxClientFactory.create(u);
        }
        throw new IllegalArgumentException("Unrecognized account type: " + u.accountType);
    }
}
