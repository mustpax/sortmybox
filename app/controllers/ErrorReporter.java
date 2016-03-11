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
import play.Logger;
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
    private static final String DSN = Play.configuration
            .getProperty("raven.dsn");

    private static final Raven RAVEN;

    static {
        Raven raven = null;
        try {
            raven = AppEngineRavenFactory.ravenInstance(DSN);
        } catch (Throwable t) {
            Logger.error(t, "Failed to init Raven. DSN: %s", DSN);
        }
        RAVEN = raven;
    }

    private static void logSentry(Throwable e, User u) {
        if (RAVEN == null) {
            Logger.info("Raven not initialized. Not sending event.");
            return;
        }

        try {
            EventBuilder eb = new EventBuilder().withMessage(e.getMessage())
                    .withLevel(Event.Level.ERROR)
                    .withSentryInterface(new ExceptionInterface(e));

            // Runtime information
            try {
                eb = eb.withExtra("play.app.id", Play.id);
                eb = eb.withExtra("namespace", Namespaced.getNamespace());
                eb = eb.withTag("version", SystemProperty.applicationVersion.get());
                eb = eb.withTag("module", ModulesServiceFactory.getModulesService().getCurrentModule());
                eb = eb.withExtra("instance", ModulesServiceFactory.getModulesService().getCurrentInstanceId());
            } catch (Throwable t) {
                Logger.error(t, "Error reading runtime information");
            }

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
            for (Header header : request.headers.values()) {
                if ("cookie".equalsIgnoreCase(header.name)) {
                    continue;
                }
                eb = eb.withExtra("Header: " + header.name, header.value());
            }

            RAVEN.sendEvent(eb.build());
            Logger.info("Sent error to Raven.");
        } catch (Throwable t) {
            Logger.error(t, "Error building/sending Raven error.");
        }
    }

    @Catch(Exception.class)
    static void logError(Throwable e) {
        User u = Login.getUser();
        Key id = u == null ? null : u.getKey();
        logSentry(e, u);
        Mails.logError(id, e, request.headers.values());
    }
}
