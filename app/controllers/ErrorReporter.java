package controllers;

import models.User;
import notifiers.Mails;
import play.mvc.Catch;
import play.mvc.Controller;

/**
 * Catch unhandled exceptions and report them via email
 * 
 * @author mustpax
 */
public class ErrorReporter extends Controller {
    @Catch(Exception.class)
    static void logError(Throwable e) {
        User u = Login.getLoggedInUser();
        Long id = u == null ? null : u.id;
        Mails.logError(id, e, request.headers.values());
    }
}
