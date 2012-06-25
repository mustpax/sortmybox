package box;

import models.User;
import models.User.AccountType;

import com.google.common.base.Preconditions;

public class BoxClientFactory {
    /**
     * @return {@link BoxClient} for the specified user.
     */
    public static BoxClient create(User user) {
        Preconditions.checkNotNull(user, "User can't be null");
        assert AccountType.BOX == user.accountType : "User must be a Dropbox user";
        return create(user.getToken());
    }

    public static BoxClient create(String token) {
        Preconditions.checkNotNull(token, "Token can't be null");
        return new BoxClientImpl(token);
    }
}
