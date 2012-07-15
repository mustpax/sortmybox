package dropbox.client;

import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.common.io.Files;

import play.Play;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS.HttpResponse;

import dropbox.gson.DbxMetadata;
import dropbox.gson.DbxAccount;

public interface DropboxClient {

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

    /**
     * Moves a file or folder to a new location.
     * 
     * @param from A file or folder to move
     * @param to The new destination
     * @return The metadata for the moved file or folder, null if there's a failure
     * 
     * @throws FileMoveCollisionException if file move failed since there is already another file with
     * the same name at the destination
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @Nullable DbxMetadata move(String from, String to) throws FileMoveCollisionException, InvalidTokenException;
    
    /**
     * Get all files, excluding directories, inside the given directory.
     * 
     * @param path path to the directory to check with the leading /
     * @return set of files (not directories) inside the directory
     * 
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @Nonnull Set<String> listDir(String path) throws InvalidTokenException, NotADirectoryException;
    
    /**
     * Get all files, excluding directories, inside the given directory.
     * 
     * @param path path to the directory, leading / optional
     * @param listingType which types of entries to return
     * @return set of files (not directories) inside the directory
     * 
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @Nonnull Set<String> listDir(String path, ListingType listingType) throws InvalidTokenException, NotADirectoryException;

    /**
     * Create a directory at the specified location 
     * @param path the full path of the directory to create
     * @return metadata for the newly created directory
     * 
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    @Nullable DbxMetadata mkdir(String path);

    /**
     * Make signed API request return the resulting HttpResponse
     * @param method HTTP method for the request
     * @param url full request URL with associated parameters
     * @return HTTP response
     */
    @Nonnull HttpResponse debug(HTTPMethod method, String url) throws InvalidTokenException;

    public static enum ListingType {
        DIRS(true, false),
        ALL(true, true),
        FILES(false, true);
        
        public final boolean includeDirs;
        public final boolean includeFiles;
        
        private ListingType(boolean includeDirs, boolean includeFiles) {
            this.includeDirs  = includeDirs;
            this.includeFiles = includeFiles;
        }
    }
}
