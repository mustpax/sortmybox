package controllers;

import java.lang.reflect.Type;
import java.util.List;

import models.Rule;
import play.mvc.Controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Rules extends Controller {
    public static void update(String rules) {
        Type t = new TypeToken<List<Rule>>(){}.getType();
        List<Rule> ruleList = new Gson().fromJson(rules, t);
        for (Rule r: ruleList) {
            System.out.println(r.toString());
        }
    }
}
