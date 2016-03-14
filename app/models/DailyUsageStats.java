package models;

import java.io.Serializable;
import java.util.Date;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.MoreObjects;

/**
 * Collects daily usage stats
 *
 * @author syyang
 */
public class DailyUsageStats implements Serializable {

    private static final long serialVersionUID = 45L;
    
    public static final Mapper<DailyUsageStats> MAPPER = new UsageDailyStatsMapper();
    public static final String KIND = "DailyUsageStats";

    public Long id;
    public Long users;
    public Long rules;
    public Long fileMoves;
    public Long uniqueFileMoveUsers;
    public Date created;
    
    /**
     * @param users
     * @param rules
     * @param fileMoves
     * @param uniqueFileMoveUsers
     */
    public DailyUsageStats(Long users, Long rules, Long fileMoves, Long uniqueFileMoveUsers) {
        this(users, rules, fileMoves, uniqueFileMoveUsers, new Date());
    }

    // exposed for tests
    public DailyUsageStats(Long users, Long rules, Long fileMoves, Long uniqueFileMoveUsers, Date created) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.uniqueFileMoveUsers = uniqueFileMoveUsers;
        this.created = created;
    }

    private DailyUsageStats(Entity entity) {
        this.id = entity.getKey().getId();
        this.users = (Long) entity.getProperty("users");
        this.rules = (Long) entity.getProperty("rules");
        this.fileMoves = (Long) entity.getProperty("fileMoves");
        this.uniqueFileMoveUsers = (Long) entity.getProperty("uniqueFileMoveUsers");
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
        return MoreObjects.toStringHelper(DailyUsageStats.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .add("uniqueFileMoveUsers", uniqueFileMoveUsers)
            .add("created", created)
            .toString();
    }

    private static class UsageDailyStatsMapper implements Mapper<DailyUsageStats> {
        @Override
        public Entity toEntity(DailyUsageStats model) {
            Entity ret = DatastoreUtil.newEntity(KIND, model.id);
            ret.setProperty("users", model.users);
            ret.setProperty("rules", model.rules);
            ret.setProperty("fileMoves", model.fileMoves);
            ret.setProperty("uniqueFileMoveUsers", model.uniqueFileMoveUsers);
            ret.setProperty("created", model.created);
            return ret;
        }

        @Override
        public DailyUsageStats toModel(Entity entity) {
            return new DailyUsageStats(entity);
        }

        @Override
        public Class<DailyUsageStats> getType() {
            return DailyUsageStats.class;
        }

        @Override
        public Key toKey(DailyUsageStats model) {
            return key(model.id);
        }
    };

}
