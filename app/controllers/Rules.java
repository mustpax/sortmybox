package controllers;

import java.lang.reflect.Type;
import java.util.List;

import models.Rule;
import models.Rule.RuleError;
import models.User;
import play.mvc.Controller;
import play.mvc.With;
import rules.RuleUtils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
            badRequest();
        }

        List<Rule> toSave = Lists.newArrayList();
        List<List<RuleError>> allErrors = Lists.newArrayList();
        boolean hasErrors = false;

        final User user = Login.getLoggedInUser();

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Transaction tx = ds.beginTransaction();
        try {
            Query q = new Query(Rule.KIND)
                .setAncestor(user.getKey())
                .setKeysOnly();
            PreparedQuery pq = ds.prepare(tx, q);

            // delete existing rules
            ds.delete(tx, Iterables.transform(pq.asIterable(), Rule.TO_KEY));

            if (ruleList.isEmpty()) {
                // in effect we are clearing all rules
                tx.commit();
            } else {
                int rank = 0;
                for (Rule rule : ruleList) {
                    rule.owner = user.id;
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
                    List<Entity> entities = Lists.transform(toSave, Rule.TO_ENTITY);
                    ds.put(tx, entities);
  
                    tx.commit();
                }
            }
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }

        if (!hasErrors && !toSave.isEmpty()) {
            RuleUtils.runRules(user);
        }

        renderJSON(allErrors);
    }
}
