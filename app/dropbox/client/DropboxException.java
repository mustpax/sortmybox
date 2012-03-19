package dropbox.client;

/**
 * Generic dropbox API exception
 */
public class DropboxException extends Exception {
    private final String message;

    public DropboxException() {
        this(null);
    }

    public DropboxException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
