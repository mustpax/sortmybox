package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import rules.RuleType;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.primitives.Ints;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * A sorting rule that allows files to be moved to a specified
 * location when certain criteria are met.
 * 
 * @author mustpax
 * @author syyang
 */
public class Rule implements Serializable {
	
    private static final long serialVersionUID = 45L;

    public static final String KIND = "Rule";
    public static final RuleMapper MAPPER = new RuleMapper();

    public static final int MAX_RULES = 200;

    private static final Comparator<Rule> RANK_COMPARATOR = new Comparator<Rule>() {
        @Override public int compare(Rule rule1, Rule rule2) {
            assert rule1 != null && rule2 != null : "rules can't be null";
            return Ints.compare(rule1.rank, rule2.rank);
        }
    };

    public long id;
    public Long owner;
    public RuleType type;
    public String pattern;
    public String dest;
    public Integer rank;

    public Rule() {}
    
    public Rule(RuleType type, String pattern, String dest, Integer rank, Long owner) {
        this.type = type;
        this.pattern = pattern;
        this.dest = dest;
        this.rank = rank;
        this.owner = owner;
    }
    
    private Rule(Entity entity) {
        this.id = entity.getKey().getId();
        this.type = RuleType.fromDbValue((String) entity.getProperty("type"));
        this.pattern = (String) entity.getProperty("pattern");
        this.dest = (String) entity.getProperty("dest");
        this.rank = ((Long) entity.getProperty("rank")).intValue();
        this.owner = (Long) entity.getProperty("owner");
    }

    /** 
     * @param userId the user id
     * @return all the rules for the given user id, sorted by rank.
     * <p>
     * NOTE: the max number of rules is bound by {@link #MAX_RULES_TO_FETCH}
     */
    public static List<Rule> findByUserId(Long userId) {
        Preconditions.checkNotNull(userId, "User id can't be null");
        Query q = byOwner(userId);

        List<Rule> rules = Lists.newArrayList(fetch(q));
        Collections.sort(rules, RANK_COMPARATOR);
        return rules;
    }
    
    public static Iterable<Rule> fetch(Query q) {
        return fetch(q, -1);
    }

    public static Iterable<Rule> fetch(Query q, int limit) {
        if (limit < 0) {
            limit = MAX_RULES;
        }

        FetchOptions fo = FetchOptions.Builder.withLimit(limit);
        return DatastoreUtil.query(q, fo, MAPPER);
    }
    
    public static Iterable<Key> fetchKeys(Query q) {
        return fetchKeys(q, -1);
    }
    
    public static Iterable<Key> fetchKeys(Query q, int limit) {
        if (limit < 0) {
            limit = MAX_RULES;
        }

        FetchOptions fo = FetchOptions.Builder.withLimit(limit);
        return DatastoreUtil.queryKeys(q, fo, MAPPER);
    }
            

    public static Query all() {
        return new Query(KIND);
    }
    
    public static Query byOwner(long userId) {
	    return all().setAncestor(User.key(userId));
    }


    public static boolean ruleExists(Long userId) {
        return fetch(byOwner(userId).setKeysOnly()).iterator().hasNext();
    }
    
    public boolean matches(String fileName) {
        return type.matches(this.pattern, fileName);
    }

    public @Nonnull List<RuleError> validate() {
        List<RuleError> ret = Lists.newLinkedList();
        if (type == null) {
            ret.add(new RuleError("type", "Missing or invalid type."));
        }

        if (owner == null) {
            ret.add(new RuleError("owner", "Missing owner."));
        }
        
        if (StringUtils.isBlank(pattern)) {
            ret.add(new RuleError("pattern", "Can't be empty."));
        } else if (pattern.contains("/")) {
            ret.add(new RuleError("pattern", "Can't contain slashes (/)."));
        }
        // Extensions may not include periods
        else if ((type == RuleType.EXT_EQ) &&
	              pattern.contains(".")) {
            ret.add(new RuleError("pattern", "Can't contain periods."));
        }

        if (StringUtils.isBlank(dest)) {
            ret.add(new RuleError("dest", "Can't be empty."));
        } else if (! dest.startsWith("/")) {
            ret.add(new RuleError("dest", "Must start with a slash (/)."));
        }
        
        // TODO check dest is not a file

        return ret;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder()
            .append(this.id)
            .append(this.type)
            .append(this.pattern)
            .append(this.dest)
            .append(this.rank)
            .append(this.owner);
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        Rule other = (Rule) obj;
        EqualsBuilder eq = new EqualsBuilder()
            .append(this.id, other.id)
            .append(this.type, other.type)
            .append(this.pattern, other.pattern)
            .append(this.dest, other.dest)
            .append(this.rank, other.rank)
            .append(this.owner, other.owner);
        return eq.isEquals();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(Rule.class)
            .add("id", id)
            .add("rule type", type)
            .add("pattern", pattern)
            .add("dest", dest)
            .add("rank", rank)
            .add("owner", owner)
            .toString();
    }

    /**
     * Replace all rules for given user with a new list of rules.
     * 
     * @param allErrors if not null, validation errors are saved here
     * @return true if there were no errors and new rules were inserted
     *              inserted
     */
    public static boolean insertAll(User user,
                                    List<Rule> ruleList,
                                    @CheckForNull List<List<RuleError>> allErrors) {
        if ((ruleList == null) || (ruleList.size() > MAX_RULES)) {
            throw new IllegalArgumentException("Rules missing or too many rules.");
        }

        List<Rule> toSave = Lists.newArrayList();
        boolean needToRun = true;
    
        List<Key> oldKeys = Lists.newArrayList(fetchKeys(byOwner(user.id)));

        if (ruleList.isEmpty()) {
            Logger.info("Deleting all rules since there are no new rules to insert.");
            // No rules inserted no need to run
            needToRun = false;
        } else {
            int rank = 0;
            for (Rule rule : ruleList) {
                rule.owner = user.id;
                List<RuleError> errors = rule.validate();
                if (errors.isEmpty()) {
                    rule.rank = rank++;
                    toSave.add(rule);
                } else {
                    needToRun = false;
                }

                if (allErrors != null) {
                    allErrors.add(errors);
                }
            }

            if (!toSave.isEmpty()) {
                Logger.info("Inserting %d new rules for user.", toSave.size());
                saveAll(toSave);
            }
        }
        
        Logger.info("Deleting %d old rules.", oldKeys.size());
        // delete existing rules
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        ds.delete(oldKeys);
    
        return needToRun;
    }
    
    public static Key key(long id) {
        return KeyFactory.createKey(KIND, id);
    }

    private static void saveAll(Iterable<Rule> rules) {
        DatastoreUtil.put(rules, MAPPER);
    }

    public static class RuleError {
        public final String field;
        public final String msg;

        public RuleError(String field, String msg) {
            this.field = field;
            this.msg = msg;
        }
    }

    private static class RuleMapper implements Mapper<Rule> {

        private RuleMapper() {}

        @Override
        public Key getKey(Rule rule) {
        	return User.key(rule.id);
        }

        @Override
        public Entity toEntity(Rule r) {
            Key parentKey = User.key(r.owner);
            Entity entity = new Entity(KIND, parentKey);
            entity.setProperty("type", r.type.name());
            entity.setProperty("pattern", r.pattern);
            entity.setProperty("dest", r.dest);
            entity.setProperty("rank", r.rank);
            entity.setProperty("owner", r.owner);
            return entity;
        }

        @Override
        public Rule toModel(Entity entity) {
            return new Rule(entity);
        }

        @Override
        public Class<Rule> getType() {
            return Rule.class;
        }

        @Override
        public Key toKey(Rule model) {
            return key(model.id);
        }
    }
}
