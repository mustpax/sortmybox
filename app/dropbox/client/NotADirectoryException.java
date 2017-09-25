package dropbox.client;

/**
 * Thrown when a directory specific action is attempted on
 * a non-directory.
 * 
 * @see DropboxClient#listDir(String)
 * 
 * @author mustpax
 */
public class NotADirectoryException extends DropboxException {
    public NotADirectoryException(String msg) {
        super(msg);
    }

    public NotADirectoryException(Throwable cause) {
        super(cause);
    }
}
