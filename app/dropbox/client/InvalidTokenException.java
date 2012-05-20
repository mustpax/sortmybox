package dropbox.client;

/**
 * Indicates that the OAuth token/secret pair for the current user
 * is no longer valid.
 * 
 * @author mustpax
 */
public class InvalidTokenException extends DropboxException {
    public InvalidTokenException() {
        super(null);
    }
}
