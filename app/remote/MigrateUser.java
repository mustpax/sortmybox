package remote;

import com.dropbox.core.DbxException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Throwables;

import common.api.ApiClient;
import common.api.ApiClientFactory;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.DropboxV2ClientImpl;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;
import models.DatastoreUtil;
import models.User;
import models.User.AccountType;
import play.Logger;

public class MigrateUser extends RemoteScript {
    /**
     * Migrate the given user to a Dropbox V2 API toke.
     * DOES NOT Update user in the AppEngine datastore. That responsibility is yours!
     * 
     * @param u user to migrate, once migrated user will be updated
     * @return true if user was migrated and should be saved to the datastore, false if user was not migrated so no save is necessary
     */
    public static boolean migrate(User u) throws DbxException {
        if (u.dropboxV2Migrated) {
            Logger.info("Skipping user %d. Already migrated.", u.id);
            return false;
        }
        u.dropboxV2Token = DropboxV2ClientImpl.upgradeOAuth1AccessToken(u.getToken(), u.getSecret());
        u.dropboxV2Migrated = true;
        Logger.info("Successfully migrated user %d.", u.id);
        return true;
    }

    @Override
    public void innerRun() {
        try {
            User u = User.findById(AccountType.DROPBOX, 642874);
            if (MigrateUser.migrate(u)) {
                Logger.info("Saved user %d to DataStore.", u.id);
                DatastoreUtil.put(u, User.MAPPER);
            }
        } catch (Throwable t) {
            Throwables.propagate(t);
        }
    }

    public static void main(String[] args) {
        new MigrateUser().run();
    }
}
