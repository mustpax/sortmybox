package models;

import java.util.Date;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.base.Objects;

public class UsageStats {
    private static final Mapper<UsageStats> MAPPER = new UsageStatsMapper();
    private static final String KIND = "UsageStats";

    public Long id;
    public Integer users;
    public Integer rules;
    public Integer fileMoves;
    public Date created;

    public UsageStats(Integer users, Integer rules, Integer fileMoves) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.created = new Date();
    }
    
    private UsageStats(Entity entity) {
        this.id = entity.getKey().getId();
        this.users = (Integer) entity.getProperty("users");
        this.rules = (Integer) entity.getProperty("rules");
        this.fileMoves = (Integer) entity.getProperty("fileMoves");
        this.created = (Date) entity.getProperty("created");
    }

    public static Key key(long id) {
        return KeyFactory.createKey(KIND, id);
    }

    public void save() {
        DatastoreUtil.put(this, MAPPER);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(User.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .toString();
    }

    private static class UsageStatsMapper implements Mapper<UsageStats> {
        @Override
        public Entity toEntity(UsageStats model) {
            Entity ret = new Entity(key(model.id));
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
    }
}
