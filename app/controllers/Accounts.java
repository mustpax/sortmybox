package controllers;

import models.CascadingDelete;
import models.User;
import notifiers.Mails;
import play.Logger;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import rules.RuleUtils;

import common.api.ApiClient;
import common.api.ApiClientFactory;
import dropbox.client.InvalidTokenException;

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
	
	/**
	 * Update sortingFolder for user.
	 */
	public static void sortingFolder(@Required String folder) {
        User u = Login.getUser();
        folder = RuleUtils.normalize(folder, false);

	    try {
	        ApiClient api = ApiClientFactory.create(u);
	        boolean createdFolder = false;
            if (! api.exists(folder)) {
                Logger.info("Folder does not exist attempting to create %s", folder);
                if (api.mkdir(folder)) {
                    Logger.info("Successfully created folder %s", folder);
                    createdFolder = true;
                } else {
                    Logger.error("Failed to create folder '%s'", folder);
                    flash.error("Error: folder %s is missing and we couldn't create it.", folder);
                    settings();
                }
            }

    	    u.sortingFolder = folder;
    	    String createdFolderMsg = createdFolder ? " This folder didn't exist, so we created it for you." : "";
    	    flash.success("%s is now your sorting folder.%s", folder, createdFolderMsg);
    	    u.save();
    	    Logger.info("Updated sorting folder to %s", folder);
    	    settings();
        } catch (InvalidTokenException e) {
            Logger.error(e, "Bad token when trying to update sorting folder to '%s' for user %s", folder, u);
            Login.logout();
        }
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
