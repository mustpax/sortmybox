package models;

import java.io.Serializable;
import java.util.Date;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Objects;

/**
 * Collects daily usage stats
 *
 * @author syyang
 */
public class UsageDailyStats implements Serializable {

    private static final long serialVersionUID = 45L;
    
    public static final Mapper<UsageDailyStats> MAPPER = new UsageDailyStatsMapper();
    private static final String KIND = "UsageDailyStats";

    public Long id;
    public Long users;
    public Long rules;
    public Long fileMoves;
    public Date created;

    public UsageDailyStats(Long users, Long rules, Long fileMoves) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.created = new Date();
    }
    
    private UsageDailyStats(Entity entity) {
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
        return Objects.toStringHelper(UsageDailyStats.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .toString();
    }
        
    private static class UsageDailyStatsMapper implements Mapper<UsageDailyStats> {
        @Override
        public Entity toEntity(UsageDailyStats model) {
            Entity ret = DatastoreUtil.newEntity(KIND, model.id);
            ret.setProperty("users", model.users);
            ret.setProperty("rules", model.rules);
            ret.setProperty("fileMoves", model.fileMoves);
            ret.setProperty("created", model.created);
            return ret;
        }

        @Override
        public UsageDailyStats toModel(Entity entity) {
            return new UsageDailyStats(entity);
        }

        @Override
        public Class<UsageDailyStats> getType() {
            return UsageDailyStats.class;
        }

        @Override
        public Key toKey(UsageDailyStats model) {
            return key(model.id);
        }
    };

}
