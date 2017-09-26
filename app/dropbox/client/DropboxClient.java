package dropbox.client;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import common.api.ApiClient;

import dropbox.gson.DbxAccount;
import dropbox.gson.DbxMetadata;

public interface DropboxClient extends ApiClient {

    /**
     * Gets information about currently logged in user's Dropbox account.
     * @throws InvalidTokenException if token is not valid or expired 
     */
    @Nonnull DbxAccount getAccount() throws InvalidTokenException;
    
    /**
     * Get file or directory metadata.
     * @param path file path
     * @return file or directory metadata, null if not found.
     * 
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @CheckForNull DbxMetadata getMetadata(String path) throws InvalidTokenException;
    
}
