package dropbox.client;

import java.util.Set;

import javax.annotation.Nonnull;

import play.Play;
import play.libs.OAuth.ServiceInfo;

import dropbox.gson.DbxMetadata;
import dropbox.gson.DbxAccount;

public interface DropboxClient {

    /**
     * Gets information about currently logged in user's Dropbox account.
     */
    @Nonnull DbxAccount getAccount();
    
    /**
     * Moves a file or folder to a new location.
     * 
     * @param from A file or folder to move
     * @param to The new destination
     * @return The metadata for the moved file or folder.
     */
    @Nonnull DbxMetadata move(String from, String to);
    
    /**
     * Get all files, excluding directories, inside the given directory.
     * 
     * @param path path to the directory to check with the leading /
     * @return set of files (not directories) inside the directory
     */
    @Nonnull Set<String> listDir(String path);

}
