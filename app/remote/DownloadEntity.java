package remote;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import common.api.ApiClient;
import common.api.ApiClientFactory;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;
import models.DatastoreUtil;
import models.User;
import models.User.AccountType;
import play.Logger;

public class DownloadEntity extends RemoteScript {
    @Override
    public void innerRun() {
        Query query = User.all();
        int i = 0;
        for (User u: DatastoreUtil.query(query, FetchOptions.Builder.withChunkSize(1000), User.MAPPER)) {
            if (u.accountType == AccountType.DROPBOX) {
                i++;
                ApiClient client = ApiClientFactory.create(u);
                try {
                    Logger.info("%04d User: %s Files: %d", i, u.id, client.listDir("/").size()); 
                } catch (InvalidTokenException | NotADirectoryException e) {
                    Logger.error(e, "Error getting directory");
                }
            }
            if (i > 5) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        new DownloadEntity().run();
    }
}
