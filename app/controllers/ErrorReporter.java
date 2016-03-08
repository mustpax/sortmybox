package controllers;

import models.User;
import notifiers.Mails;
import play.Play;
import play.mvc.Catch;
import play.mvc.Controller;
import play.mvc.Http.Header;

import java.util.Map;

import com.getsentry.raven.Raven;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.appengine.AppEngineRavenFactory;
import com.getsentry.raven.appengine.event.helper.AppEngineEventBuilderHelper;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import com.google.appengine.api.datastore.Key;

/**
 * Catch unhandled exceptions and report them via email
 * 
 * @author mustpax
 */
public class ErrorReporter extends Controller {
    private static final String DSN = Play.configuration.getProperty("raven.dsn");

    private static final Raven RAVEN = AppEngineRavenFactory.ravenInstance(DSN);

    private static void logSentry(Throwable e, User u) {
        EventBuilder eb = new EventBuilder().withMessage(e.getMessage())
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(e));
        
        if (u != null) {
            eb = eb.withTag("id", u.getKey().toString());
        }
        if (request.url != null) {
            eb = eb.withTag("url", request.url);
        }
        if (request.remoteAddress != null) {
            eb = eb.withTag("ip_address", request.remoteAddress);
        }
        for (Header header: request.headers.values()) {
            eb = eb.withTag("Header: " + header.name, header.value());
        }

        RAVEN.sendEvent(eb.build());
    }

    @Catch(Exception.class)
    static void logError(Throwable e) {
        User u = Login.getUser();
        Key id  = u == null ? null : u.getKey();
        Mails.logError(id, e, request.headers.values());
    }
}
