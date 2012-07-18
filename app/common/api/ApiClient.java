package common.api;

import java.util.Set;

import javax.annotation.Nonnull;

import play.libs.WS.HttpResponse;

import com.google.appengine.api.urlfetch.HTTPMethod;

import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

/**
 * Generic API client for talking to external services.
 *
 * @author paksoy
 */
public interface ApiClient {
    /**
     * Moves a file or folder to a new location.
     * 
     * @param from path to file or folder to move
     * @param to fully qualified new path for file/folder (must include name of the file/folder itself)
     * 
     * @throws FileMoveCollisionException if file move failed since there is already another file with
     * the same name at the destination
     * @throws InvalidTokenException if OAuth token for the current user is not valid
     */
    void move(String from, String to) throws FileMoveCollisionException, InvalidTokenException;
    
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
    @Nonnull Set<String> listDir(String path, ApiClient.ListingType listingType) throws InvalidTokenException, NotADirectoryException;

    /**
     * Create a directory at the specified location 
     * @param path the full path of the directory to create
     * @return true if folder was created successfully, false otherwise
     */
    boolean mkdir(String path);

    /**
     * Check if a file or folder exists at the location.
     * 
     * @return true if file or folder exists at path, false otherwise.
     */
    boolean exists(String path) throws InvalidTokenException;

    /**
     * Make signed API request return the resulting HttpResponse
     * @param method HTTP method for the request
     * @param url request URL with the API root host omitted and the leading slash
     *            included (like /1/metadata)
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
