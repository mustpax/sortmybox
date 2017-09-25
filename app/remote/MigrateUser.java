package remote;

import java.util.List;

import com.dropbox.core.DbxException;
import com.dropbox.core.InvalidAccessTokenException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.repackaged.com.google.api.client.util.Lists;
import com.google.common.base.Throwables;

import common.api.ApiClient;
import common.api.ApiClientFactory;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.DropboxV2ClientImpl;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;
import dropbox.gson.DbxAccount;
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
        if (u.accountType != AccountType.DROPBOX) {
            Logger.info("Skipping user. Not a Dropbox user");
            return false;
        }
        if (u.periodicSort != Boolean.TRUE) {
            Logger.info("Skipping user. User sorting disabled");
            return false;
        }
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
        List<User> toSave = Lists.newArrayList();
        // You can't filter for null fields because they are not in the index
//        int lastId = 42887095;
        int lastId = 1;
        Filter idFilter = new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.GREATER_THAN_OR_EQUAL, User.key(AccountType.DROPBOX, lastId));
        for (User u: DatastoreUtil.query(User.all().setFilter(idFilter),
                                        FetchOptions.Builder.withChunkSize(1000),
                                        User.MAPPER)) {
            boolean modified = false;
            try {
                if (MigrateUser.migrate(u)) {
                    modified = true;
                }
                if (u.dropboxV2Migrated && u.dropboxV2Id == null) {
                    Logger.info("Adding v2 user info to user %s", u.id);
                    DropboxClient client = new DropboxV2ClientImpl(u.dropboxV2Token);
                    DbxAccount acct = client.getAccount();
                    u.dropboxV2Id = acct.id;
                    u.email = acct.email;
                    modified = true;
                }
            } catch (InvalidTokenException | InvalidAccessTokenException e) {
                Logger.info("User %d token invalid, disabling user", u.id);
                u.periodicSort = false;
                modified = true;
            } catch (DbxException e) {
                Logger.error(e, "Error migrating user %d", u.id);
            }
            if (modified) {
                toSave.add(u);
            }
            if (toSave.size() >= 100) {
                DatastoreUtil.put(toSave, User.MAPPER);
                Logger.info("Saving %d users to Datastore", toSave.size());
                toSave.clear();
            }
        }
        if (toSave.size() > 0) {
            DatastoreUtil.put(toSave, User.MAPPER);
            Logger.info("Saving %d users to Datastore", toSave.size());
            toSave.clear();
        }
    }

    public static void main(String[] args) {
        new MigrateUser().run();
    }
}
