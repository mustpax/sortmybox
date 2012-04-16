package notifiers;

import com.google.common.base.Preconditions;

import models.User;
import play.mvc.Mailer;

public class Mails extends Mailer {

    public static final String NOREPLY_ADDRESS = "noreplysortbox@gmail.com";

    public static void notifyAccountDeletion(User user) {
        Preconditions.checkNotNull(user);
        setSubject("Sortbox account deactivated, " +  user.email);
        addRecipient(user.email);
        setFrom("Sortbox <" + NOREPLY_ADDRESS + ">");
        send(user);
    }

}
