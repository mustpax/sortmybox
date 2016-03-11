package controllers;

import com.getsentry.raven.Raven;
import com.getsentry.raven.appengine.AppEngineRavenFactory;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.modules.ModulesServiceFactory;
import com.google.appengine.api.utils.SystemProperty;

import models.User;
import notifiers.Mails;
import play.Play;
import play.mvc.Catch;
import play.mvc.Controller;
import play.mvc.Http.Header;

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
        
        // Runtime information
        eb = eb.withTag("version", SystemProperty.applicationVersion.get());
        eb = eb.withTag("module", ModulesServiceFactory.getModulesService().getCurrentModule());
        eb = eb.withExtra("namespace", Namespaced.getNamespace());
        eb = eb.withExtra("play.app.id", Play.id);
        
        // Request information
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
            if ("cookie".equalsIgnoreCase(header.name)) {
                continue;
            }
            eb = eb.withExtra("Header: " + header.name, header.value());
        }

        RAVEN.sendEvent(eb.build());
    }

    @Catch(Exception.class)
    static void logError(Throwable e) {
        User u = Login.getUser();
        Key id  = u == null ? null : u.getKey();
        logSentry(e, u);
        Mails.logError(id, e, request.headers.values());
    }
}
