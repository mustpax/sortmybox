package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import play.Logger;
import rules.RuleType;
import rules.RuleUtils;

import controllers.Login;

/**
 * A sorting rule that allows files to be moved to a specified
 * location when certain criteria are met.
 * 
 * @author mustpax
 * @author syyang
 */
public class Rule implements Serializable {

    public static final String KIND = "Rule";

    public static final Function<Entity, Key> TO_KEY = new Function<Entity, Key>() {
        @Override
        public Key apply(Entity entity) {
            return entity.getKey();
        }
    };

    public static final Function<Rule, Entity> TO_ENTITY = new Function<Rule, Entity>() {
        @Override
        public Entity apply(Rule rule) {
            return rule.toEntity();
        }
    };

    private static final Function<Entity, Rule> TO_RULE = new Function<Entity, Rule>() {
        @Override
        public Rule apply(Entity entity) {
            return new Rule(entity);
        }
    };

    public Long id;
    public RuleType type;
    public String pattern;
    public String dest;
    public Integer rank;
    public Long owner;

    public Rule() {}
    
    public Rule(RuleType type, String pattern, String dest, Integer rank, Long owner) {
        this.type = type;
        this.pattern = pattern;
        this.dest = dest;
        this.rank = rank;
        this.owner = owner;
    }

    public Rule(Entity entity) {
        this.id = entity.getKey().getId();
        this.type = RuleType.fromDbValue((String) entity.getProperty("type"));
        this.pattern = (String) entity.getProperty("pattern");
        this.dest = (String) entity.getProperty("dest");
        this.rank = ((Long) entity.getProperty("rank")).intValue();
        this.owner = (Long) entity.getProperty("owner");
    }

    public Entity toEntity() {
        Key parentKey = KeyFactory.createKey(User.KIND, owner);
        Entity entity = new Entity(KIND, parentKey);
        entity.setProperty("type", type.getDbValue());
        entity.setProperty("pattern", pattern);
        entity.setProperty("dest", dest);
        entity.setProperty("rank", rank);
        entity.setProperty("owner", owner);
        return entity;
    }

    public static Rule findById(User owner, Long id) {
        Preconditions.checkNotNull(id);
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        try {
            Key key = KeyFactory.createKey(owner.getKey(), KIND, id);
            return new Rule(ds.get(key));
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public static Iterator<Rule> findByOwner(User owner) {
        Preconditions.checkNotNull(owner);
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Query q = new Query(KIND).setAncestor(owner.getKey())
                                 .addSort("rank");
        PreparedQuery pq = ds.prepare(q);
        return Iterators.transform(pq.asIterator(), TO_RULE);
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
