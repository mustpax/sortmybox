package dropbox.client;

/**
 * Failure to move a file because another file with the same
 * name already exists at the location.
 * 
 * @author mustpax
 */
public class FileMoveCollisionException extends DropboxException {
    public FileMoveCollisionException(String message) {
        super(message);
    }
}
