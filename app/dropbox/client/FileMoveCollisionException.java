package dropbox.client;

import com.dropbox.core.v2.files.RelocationErrorException;

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

    public FileMoveCollisionException(RelocationErrorException e) {
        super(e);
    }
}
