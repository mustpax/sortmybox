package models;

import java.io.Serializable;
import java.util.Date;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.MoreObjects;

public class UsageStats implements Serializable {

    private static final long serialVersionUID = 45L;

    public static final Mapper<UsageStats> MAPPER = new UsageStatsMapper();
    private static final String KIND = "UsageStats";

    public Long id;
    public Long users;
    public Long rules;
    public Long fileMoves;
    public Date created;

    public UsageStats(Long users, Long rules, Long fileMoves) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.created = new Date();
    }
    
    private UsageStats(Entity entity) {
        this.id = entity.getKey().getId();
        this.users = (Long) entity.getProperty("users");
        this.rules = (Long) entity.getProperty("rules");
        this.fileMoves = (Long) entity.getProperty("fileMoves");
        this.created = (Date) entity.getProperty("created");
    }

    public static Key key(long id) {
        return KeyFactory.createKey(KIND, id);
    }

    public static Query all() {
        return new Query(KIND);
    }

    public void save() {
        DatastoreUtil.put(this, MAPPER);
    }
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(UsageStats.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .toString();
    }

    private static class UsageStatsMapper implements Mapper<UsageStats> {
        @Override
        public Entity toEntity(UsageStats model) {
            Entity ret = DatastoreUtil.newEntity(KIND, model.id);
            ret.setProperty("users", model.users);
            ret.setProperty("rules", model.rules);
            ret.setProperty("fileMoves", model.fileMoves);
            ret.setProperty("created", model.created);
            return ret;
        }

        @Override
        public UsageStats toModel(Entity entity) {
            return new UsageStats(entity);
        }

        @Override
        public Class<UsageStats> getType() {
            return UsageStats.class;
        }

        @Override
        public Key toKey(UsageStats model) {
            return key(model.id);
        }
    }
}
