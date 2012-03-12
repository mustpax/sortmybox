package controllers;

import java.util.List;

import models.Move;
import models.Rule;
import models.User;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static void index() {
        User user = RequiresLogin.getLoggedInUser();
        List<Rule> rules = Rule.findByOwner(user).fetch();
        List<Move> moves = user.getMoves().limit(10).fetch();
        render(user, rules, moves);
    }
    
    public static void process() {
        checkAuthenticity();
        RequiresLogin.getLoggedInUser().runRules();
        index();
    }
}
