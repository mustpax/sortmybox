package controllers;

import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

@With(Login.class)
public class Accounts extends Controller {

	private static final String DELETED_ACCOUNT = "deletedAccount";
	
	public static void info() {
		User user = Login.getLoggedInUser();
		render(user);
	}

	public static void settings() {
		User user = Login.getLoggedInUser();
		render(user);
	}

	public static void confirmDelete() {
		User user = Login.getLoggedInUser();
		render(user);
	}
	
    public static void delete() {
        session.clear();
        flash.put(DELETED_ACCOUNT, "true");
        Login.login();
    }

}
