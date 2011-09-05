package controllers;

import play.mvc.Controller;
import play.mvc.With;
import dropbox.Dropbox;
import dropbox.gson.DbxUser;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static void index() {
        String token = session.get("token");
        String secret = session.get("secret");
        Dropbox d = new Dropbox(token, secret);
        DbxUser user = d.getUser();
        render(user);
    }
}