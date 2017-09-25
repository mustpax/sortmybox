package dropbox.client;

/**
 * Indicates that the OAuth token/secret pair for the current user
 * is no longer valid.
 * 
 * @author mustpax
 */
public class InvalidTokenException extends DropboxException {
    public InvalidTokenException(Exception cause) {
        super(cause);
    }

    public InvalidTokenException(String msg) {
        super(msg);
    }
}
