package dropbox.client;

import java.util.HashSet;
import java.util.Set;

import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxOAuth1AccessToken;
import com.dropbox.core.DbxOAuth1Upgrader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.CreateFolderErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.RelocationErrorException;
import com.dropbox.core.v2.users.FullAccount;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.common.base.Throwables;

import dropbox.Dropbox;
import dropbox.gson.DbxAccount;
import dropbox.gson.DbxMetadata;
import play.Logger;
import play.libs.WS.HttpResponse;

public class DropboxV2ClientImpl implements DropboxClient {
    private final DbxClientV2 dbxClient;

    public DropboxV2ClientImpl(String token) {
        dbxClient = new DbxClientV2(Dropbox.REQ_CONFIG, token);
    }

    @Override
    public void move(String from, String to)
            throws FileMoveCollisionException, InvalidTokenException {
        try {
            dbxClient.files().move(from, to);
        } catch (RelocationErrorException e) {
            if (e.errorValue.isTo() && e.errorValue.getToValue().isConflict()) {
                throw new FileMoveCollisionException(e);
            }
            Throwables.propagate(e);
        } catch (DbxException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public Set<String> listDir(String path)
            throws InvalidTokenException, NotADirectoryException {
        return listDir(path, ListingType.FILES);
    }

    @Override
    public Set<String> listDir(String path, ListingType listingType)
            throws InvalidTokenException, NotADirectoryException {
        // Dropbox expects root folder to be specified as empty string
        if (path.equals("/")) {
            path = "";
        }

        Set<String> ret = new HashSet<>();
        try {
            ListFolderResult result = null;
            do {
                if (result == null) {
                    result = dbxClient.files().listFolder(path);
                } else {
                    result = dbxClient.files().listFolderContinue(result.getCursor());
                }
                for (Metadata f: result.getEntries()) {
                    if ((f instanceof FileMetadata && listingType.includeFiles) ||
                            (f instanceof FolderMetadata && listingType.includeDirs)){
                        ret.add(f.getPathDisplay());
                    }
                }
            } while (result.getHasMore());
        } catch (ListFolderErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFolder()) {
                throw new NotADirectoryException(e);
            } else {
                Throwables.propagate(e);
            }
        } catch (DbxException e) {
            Throwables.propagate(e);
        }
        return ret;
    }

    @Override
    public boolean mkdir(String path) throws InvalidTokenException {
        try {
            dbxClient.files().createFolder(path);
        } catch (DbxException e) {
            Throwables.propagate(e);
        }
        return true;
    }

    @Override
    public boolean exists(String path) throws InvalidTokenException {
        return getMetadata(path) != null;
    }

    @Override
    public HttpResponse debug(HTTPMethod method, String url)
            throws InvalidTokenException {
        throw new UnsupportedOperationException("Debug endpoint is not supported with the v2 version of the API");
    }

    @Override
    public DbxAccount getAccount() {
        try {
            FullAccount account = dbxClient.users().getCurrentAccount();
            DbxAccount ret = new DbxAccount();
            ret.name = account.getName().getDisplayName();
            ret.id = account.getAccountId();
            ret.email = account.getEmailVerified() ? account.getEmail() : null;
            return ret;
        } catch (DbxException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    @Override
    public DbxMetadata getMetadata(String path) throws InvalidTokenException {
        try {
            Metadata metadata = dbxClient.files().getMetadata(path);
            DbxMetadata ret = new DbxMetadata();
            ret.path = metadata.getPathDisplay();
            ret.isDir = metadata instanceof FolderMetadata;
            return ret;
        } catch (GetMetadataErrorException e) {
            if (e.errorValue.isPath() && e.errorValue.getPathValue().isNotFound()) {
                // getMetadata() is expected to return if path is not found
                return null;
            }
            Throwables.propagate(e);
        } catch (DbxException e) {
            Throwables.propagate(e);
        }
        return null;
    }

    public static String upgradeOAuth1AccessToken(String token, String secret) throws DbxException {
        DbxOAuth1AccessToken authToken = new DbxOAuth1AccessToken(token, secret);
        DbxOAuth1Upgrader upgrader = new DbxOAuth1Upgrader(Dropbox.REQ_CONFIG, Dropbox.APP_INFO);
        return upgrader.createOAuth2AccessToken(authToken);
    }
}
