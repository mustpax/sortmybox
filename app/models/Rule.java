package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.data.validation.Required;
import play.modules.objectify.Datastore;
import play.modules.objectify.ObjectifyModel;
import rules.RuleType;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.repackaged.com.google.common.primitives.Ints;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Parent;

/**
 * A sorting rule that allows files to be moved to a specified
 * location when certain criteria are met.
 * 
 * @author mustpax
 * @author syyang
 */
public class Rule extends ObjectifyModel implements Serializable {

    private static final int MAX_RULES_TO_FETCH = 10;

    private static final Comparator<Rule> RANK_COMPARATOR = new Comparator<Rule>() {
        @Override public int compare(Rule rule1, Rule rule2) {
            assert rule1 != null && rule2 != null : "rules can't be null";
            return Ints.compare(rule1.rank, rule2.rank);
        }
    };

    @Id public Long id;
    @Required @Parent public Key<User> owner;
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
        this.owner = Datastore.key(User.class, owner);
    }

    public static Rule findById(Long userId, Long ruleId) {
        Key<Rule> ruleKey = Datastore.key(User.class, userId, Rule.class, ruleId);
        return Datastore.find(ruleKey, false);
    }

    /** 
     * @param userId the user id
     * @return all the rules for the given user id, sorted by rank.
     * <p>
     * NOTE: the max number of rules is bound by {@link #MAX_RULES_TO_FETCH}
     */
    public static List<Rule> findByUserId(Long userId) {
        Preconditions.checkNotNull(userId, "User id can't be null");
        Iterator<Rule> itr = Datastore.query(Rule.class)
                .ancestor(Datastore.key(User.class, userId))
                .limit(MAX_RULES_TO_FETCH).iterator();
        List<Rule> rules = Lists.newArrayList(itr);
        Collections.sort(rules, RANK_COMPARATOR);
        return rules;
    }

    public static boolean ruleExists(Long userId) {
        QueryResultIterable<Key<Rule>> ruleKeys = Datastore
            .query(Rule.class)
            .ancestor(Datastore.key(User.class, userId))
            .limit(1)
            .fetchKeys();
        return ruleKeys.iterator().hasNext();
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
            ret.add(new RuleError("owner", "Missing rule owner."));
        }
        
        if (StringUtils.isBlank(pattern)) {
            ret.add(new RuleError("pattern", "Pattern cannot be empty."));
        } else if (pattern.contains("/")) {
            ret.add(new RuleError("pattern", "Pattern cannot contain slashes (/)."));
        }
        // Extensions may not include periods
        else if ((type == RuleType.EXT_EQ) &&
	              pattern.contains(".")) {
            ret.add(new RuleError("pattern", "Extensions cannot contain periods."));
        }

        if (StringUtils.isBlank(dest)) {
            ret.add(new RuleError("dest", "Destination directory cannot be empty."));
        } else if (! dest.startsWith("/")) {
            ret.add(new RuleError("dest", "Destination directory must start with a slash (/)."));
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
        if (!super.equals(obj))
            return false;
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

    public static class RuleError {
        public final String field;
        public final String msg;

        public RuleError(String field, String msg) {
            this.field = field;
            this.msg = msg;
        }
    }
}
