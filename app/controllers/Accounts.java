package controllers;

import models.CascadingDelete;
import models.User;
import notifiers.Mails;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With(Login.class)
public class Accounts extends Controller {
	public static void settingsPost(boolean periodicSort) {
	    checkAuthenticity();
	    User user = Login.getUser();
	    user.periodicSort = periodicSort;
	    user.save();
	    if (periodicSort) {
		    flash.success("Periodic sort enabled.");
	    } else {
		    flash.success("Periodic sort disabled.");
	    }
	    Logger.info("Settings updated for user: %s", user);
	    settings();
	}
	
	public static void settings() {
	    User user = Login.getUser();
	    render(user);
	}

	public static void delete() {
		User user = Login.getUser();
		String contact = Mails.CONTACT_EMAIL;
		render(user, contact);
	}
	
    public static void deletePost() {
        checkAuthenticity();
        User user = Login.getUser();

        CascadingDelete.delete(user);

        session.clear();
        flash.success("Account deleted successfully.");

        Login.login();
    }

}
