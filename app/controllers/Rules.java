package controllers;

import java.lang.reflect.Type;
import java.util.List;

import models.Rule;
import models.Rule.RuleError;
import models.User;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@With(RequiresLogin.class)
public class Rules extends Controller {
    public static void update(String rules) {
        checkAuthenticity();
        Type t = new TypeToken<List<Rule>>(){}.getType();
        List<Rule> ruleList = new Gson().fromJson(rules, t);
        User user = RequiresLogin.getLoggedInUser();
        Rule.findByOwner(user).delete();
        List<List<RuleError>> errors = Rule.insert(user, ruleList);
        if (! hasErrors(errors)) {
            user.runRules();
        }
        renderJSON(errors);
    }

    /**
     * @return if any of the rules have any errors
     */
    private static boolean hasErrors(List<List<RuleError>> errors) {
        return Iterables.any(errors, new Predicate<List>() {
            @Override
            public boolean apply(List l) {
                return ! l.isEmpty();
            }
        });
    }
}
