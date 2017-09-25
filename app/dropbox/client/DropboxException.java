package dropbox.client;

/**
 * Generic dropbox API exception
 */
public class DropboxException extends Exception {
    public DropboxException() {
        this((String) null);
    }

    public DropboxException(String message) {
        super(message);
    }

    public DropboxException(Throwable cause) {
        super(cause);
    }
}
