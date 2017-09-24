package remote;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import models.DatastoreUtil;
import models.User;
import play.Logger;

public class DownloadEntity extends RemoteScript {
    @Override
    public void innerRun() {
        Query query = User.all();
        int i = 0;
        for (User u: DatastoreUtil.query(query, FetchOptions.Builder.withChunkSize(1000), User.MAPPER)) {
            Logger.info("%04d User: %s", i, u.id); 
            i++;
            if (i > 2000) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        new DownloadEntity().run();
    }
}
