package dropbox.client;

import com.google.common.base.Preconditions;

import models.User;
import controllers.RequiresLogin;

/**
 * Factory for creating a {@link DropboxClient} implementation.
 */
public class DropboxClientFactory {
    
    /**
     * @return DropboxClient for the specified user.
     */
    public static DropboxClient create(User user) {
        Preconditions.checkNotNull(user, "User can't be null");
        return new DropboxClientImpl(user.token, user.secret);
    }
    
    /**
     * @return DropboxClient using the specified credential
     */
    public static DropboxClient create(String token, String secret) {
        Preconditions.checkNotNull(token, "Token can't be null");
        Preconditions.checkNotNull(secret, "Secret can't be null");
        return new DropboxClientImpl(token, secret);
    }
    
    private DropboxClientFactory() {}
}
