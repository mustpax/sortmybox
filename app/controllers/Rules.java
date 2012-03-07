package controllers;

import java.lang.reflect.Type;
import java.util.List;

import models.Rule;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@With(RequiresLogin.class)
public class Rules extends Controller {
    public static void update(String rules) {
        checkAuthenticity();
        Type t = new TypeToken<List<Rule>>(){}.getType();
        List<Rule> ruleList = new Gson().fromJson(rules, t);
        Rule.findByOwner(RequiresLogin.getLoggedInUser()).delete();
        renderJSON(Rule.insert(ruleList));
    }
}
