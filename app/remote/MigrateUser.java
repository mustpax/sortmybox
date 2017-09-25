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
    @Override
    public void innerRun() {
        try {
            User u = User.findById(AccountType.DROPBOX, 642874);
            String v2Token = DropboxV2ClientImpl.upgradeOAuth1AccessToken(u.getToken(), u.getSecret());
            Logger.info("Upgaded token: %s", v2Token);
            DropboxClient client = new DropboxV2ClientImpl(v2Token);
            Logger.info("Files for user: %d", client.listDir("/space mountain.jpg").size());
        } catch (Throwable t) {
            Throwables.propagate(t);
        }
    }

    public static void main(String[] args) {
        new MigrateUser().run();
    }
}
