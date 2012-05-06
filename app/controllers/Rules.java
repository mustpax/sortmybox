package controllers;

import java.lang.reflect.Type;
import java.util.List;

import models.Rule;
import models.Rule.RuleError;
import models.User;
import play.Logger;
import play.modules.objectify.Datastore;
import play.mvc.Controller;
import play.mvc.With;
import rules.RuleUtils;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

@With(Login.class)
public class Rules extends Controller {
    
    private static final int MAX_NUM_RULES = 200;
    
    public static void update(String rules) {
        checkAuthenticity();
        Type t = new TypeToken<List<Rule>>(){}.getType();
        List<Rule> ruleList = new Gson().fromJson(rules, t);
        
        if (ruleList != null && ruleList.size() > MAX_NUM_RULES) {
            //TODO:
            //  1. preemptively check the count on the client side
            //  2. we should display an appropriate error message
            //     rather than issuing 400 error
            Logger.warn("User missing rule list or too many rules.");
            badRequest();
        }

        User user = Login.getLoggedInUser();
        List<List<RuleError>> allErrors = Lists.newArrayList();

        if (insertAll(user, ruleList, allErrors)) {
            RuleUtils.runRules(user);
        }

        renderJSON(allErrors);
    }

    public static boolean insertAll(User user,
                                    List<Rule> ruleList,
                                    List<List<RuleError>> allErrors) {
        List<Rule> toSave = Lists.newArrayList();
        boolean hasErrors = false;

        Objectify ofy = Datastore.beginTxn();
        try {
            Iterable<Key<Rule>> ruleKeys = Rule.getByOwner(user)
								               .fetchKeys();
            
            // delete existing rules
            Datastore.delete(ruleKeys);

            if (ruleList.isEmpty()) {
                // in effect we are clearing all rules
                Datastore.commit();
            } else {
                int rank = 0;
                for (Rule rule : ruleList) {
                    rule.owner = user.getKey();
                    List<RuleError> errors = rule.validate();
                    if (errors.isEmpty()) {
                        rule.rank = rank++;
                        toSave.add(rule);
                    } else {
                        hasErrors = true;
                    }
                    allErrors.add(errors);
                }
                if (!toSave.isEmpty()) {
                    Datastore.put(toSave);
                    Datastore.commit();
                }
            }
            return hasErrors && !toSave.isEmpty();
        } finally {
            if (ofy.getTxn().isActive()) {
                ofy.getTxn().rollback();
            }
        }
    }
}
