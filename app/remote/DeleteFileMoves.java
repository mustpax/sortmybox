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
import cron.FileMoveSweeper;
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

public class DeleteFileMoves extends RemoteScript {

    @Override
    public void innerRun() {
        FileMoveSweeper.deleteOldFileMoves();
    }

    public static void main(String[] args) {
        new DeleteFileMoves().run();
    }
}
