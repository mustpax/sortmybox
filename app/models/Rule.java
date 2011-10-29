package models;

import java.util.ArrayList;
import java.util.List;

import play.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class Rule {
    public RuleType type;
    public String pattern;
    public String dest;
    
    @Override
    public String toString() {
        return String.format("Rule Type: %s Pattern: '%s' Dest: '%s'",
                             this.type, this.pattern, this.dest);
    }
    
    public static Iterable<Entity> getAll() {
        Query q = new Query("rule");
        q.addSort("rank");
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pq = datastore.prepare(q);
        
        return pq.asIterable();
    }

    public static void saveRules(List<Rule> rules) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.delete(Iterables.transform(getAll(), new KeyExtractor()));
        
        if (rules == null || rules.isEmpty()) {
            return;
        }
        
        int rank = 0;
        List<Entity> ents = new ArrayList<Entity>(rules.size());
        for (Rule r: rules) {
            Entity rule = new Entity("rule");
            rule.setProperty("rank", rank++);
            rule.setProperty("type", r.type.name());
            rule.setProperty("pattern", r.pattern);
            rule.setProperty("dest", r.dest);
            ents.add(rule);
            Logger.info("Processed rule: %s", rule);
        }
        
        Logger.info("Saved entities: %s:", datastore.put(ents));
    }
    
    public static class KeyExtractor implements Function<Entity, Key> {
        @Override
        public Key apply(Entity ent) {
            return ent.getKey();
        }
    }
    
    public static enum RuleType {
        NAME_CONTAINS {
            @Override
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
            }
        },
        NAME_GLOB {
            @Override
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
            }
        },
        EXT_EQ {
            @Override 
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
            }
        };

        /**
         * Apply a match to a file based on the rule type.
         * 
         * @param pattern the pattern for the current rule
         * @param fileName file name to match against rule
         * @return true if given file name matches the current pattern
         */
        public abstract boolean matches(String pattern, String fileName);
    }
}
