package controllers;

import models.User;
import notifiers.Mails;
import play.mvc.Catch;
import play.mvc.Controller;

import com.google.appengine.api.datastore.Key;

/**
 * Catch unhandled exceptions and report them via email
 * 
 * @author mustpax
 */
public class ErrorReporter extends Controller {
    @Catch(Exception.class)
    static void logError(Throwable e) {
        User u = Login.getUser();
        Key id  = u == null ? null : u.getKey();
        Mails.logError(id, e, request.headers.values());
    }
}
