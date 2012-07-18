package dropbox.client;

import models.User;
import models.User.AccountType;
import play.Play;

import com.google.common.base.Preconditions;

/**
 * Factory for creating a {@link DropboxClient} implementation.
 */
public class DropboxClientFactory {
    public static DropboxClient testClient = null;
    
    /**
     * @return DropboxClient for the specified user.
     */
    public static DropboxClient create(User user) {
        if (Play.runingInTestMode() && testClient != null) {
            return testClient;
        }

        Preconditions.checkNotNull(user, "User can't be null");
        assert AccountType.DROPBOX == user.accountType : "User must be a Dropbox user";
        return create(user.getToken(), user.getSecret());
    }
    
    /**
     * @return DropboxClient using the specified credential
     */
    public static DropboxClient create(String token, String secret) {
        if (Play.runingInTestMode() && testClient != null) {
            return testClient;
        }

        Preconditions.checkNotNull(token, "Token can't be null");
        Preconditions.checkNotNull(secret, "Secret can't be null");
        return new DropboxClientImpl(token, secret);
    }
    
    private DropboxClientFactory() {}
}
