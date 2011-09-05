package controllers;

import play.*;
import play.libs.WS;
import play.libs.WS.WSRequest;
import play.mvc.*;

import java.util.*;

import dropbox.Dropbox;

import models.*;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static void index() {
        String token = session.get("token");
        String secret = session.get("secret");
        WSRequest ws = WS.url("https://api.dropbox.com/0/account/info").oauth(Dropbox.OAUTH, token, secret);
        String resp = ws.get().getString();
        render(resp);
    }
}