package common.api;

import java.util.Set;

import javax.annotation.Nonnull;

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
     * @param from A file or folder to move
     * @param to The new destination
     * @return The metadata for the moved file or folder, null if there's a failure
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
