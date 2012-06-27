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

    @Override
    public String getMessage() {
        return message;
    }
}
