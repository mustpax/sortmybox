package notifiers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import models.User;
import play.Logger;
import play.Play;
import play.mvc.Http.Header;
import play.mvc.Mailer;

import com.google.common.base.Preconditions;

/**
 * Send emails.
 *
 * @author mustpax
 * @author syyang
 */
public class Mails extends Mailer {
    public static final String ERROR_EMAIL = Play.configuration.getProperty("sortbox.error_email");
    public static final String FROM_EMAIL = Play.configuration.getProperty("sortbox.email");

    public static void notifyAccountDeletion(User user) {
        Preconditions.checkNotNull(user);
        setSubject("Sortbox account deactivated, " +  user.email);
        addRecipient(user.email);
        setFrom("Sortbox <" + FROM_EMAIL + ">");
        send(user);
    }
    
    public static class EmailedException implements Iterable<EmailedException> {
        public final Throwable t;
        public EmailedException(Throwable t) {
            this.t = t;
        }

        private static class Iter implements Iterator<EmailedException> {
            private Throwable current;

            public Iter(Throwable t) {
                current = t;
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public EmailedException next() {
                EmailedException ret = new EmailedException(current);
                current = current.getCause();
                return ret;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public Iterator<EmailedException> iterator() {
            return new Iter(this.t);
        }

        /**
         * @return formatted stack trace for exception
         */
        public String getStackTrace() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            this.t.printStackTrace(pw);
            return sw.toString();
        }
    }

    /**
     * Log unexpected errors by emailing them to a designated address.
     *
     * @param id user id
     * @param e exception to log
     * @param headers request headers for this request
     */
    public static void logError(Long id, Throwable e, Collection<Header> headers) {
        Logger.error(e, "Sending Gack to %s", ERROR_EMAIL);
        Date date = new Date();
        setFrom(FROM_EMAIL);
        addRecipient(ERROR_EMAIL);
        setSubject(String.format("Error at %s logged: %s", date, e.getClass()));
        EmailedException errors = new EmailedException(e);
        send(errors, id, headers, date);
    }
}
