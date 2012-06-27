package dropbox.client;

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.io.Files;
import common.api.ApiClient;

import play.Play;
import play.libs.OAuth.ServiceInfo;

import dropbox.gson.DbxMetadata;
import dropbox.gson.DbxAccount;

public interface DropboxClient extends ApiClient {

    /**
     * Gets information about currently logged in user's Dropbox account.
     */
    @Nonnull DbxAccount getAccount();
    
    /**
     * Get file or directory metadata.
     * @param path file path
     * @return file or directory metadata, null if not found.
     * 
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @CheckForNull DbxMetadata getMetadata(String path) throws InvalidTokenException;
}
