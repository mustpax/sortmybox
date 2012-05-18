package models;

import java.io.Serializable;
import java.util.Date;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Objects;

public class UsageDeltaStats implements Serializable {

    private static final long serialVersionUID = 45L;
    
    public static final Mapper<UsageDeltaStats> MAPPER = new UsageDeltaStatsMapper();
    private static final String KIND = "UsageDeltaStats";

    public Long id;
    public Long users;
    public Long rules;
    public Long fileMoves;
    public Date created;

    public UsageDeltaStats(Long users, Long rules, Long fileMoves) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.created = new Date();
    }
    
    private UsageDeltaStats(Entity entity) {
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
        return Objects.toStringHelper(UsageDeltaStats.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .toString();
    }
        
    private static class UsageDeltaStatsMapper implements Mapper<UsageDeltaStats> {
        @Override
        public Entity toEntity(UsageDeltaStats model) {
            Entity ret = DatastoreUtil.newEntity(KIND, model.id);
            ret.setProperty("users", model.users);
            ret.setProperty("rules", model.rules);
            ret.setProperty("fileMoves", model.fileMoves);
            ret.setProperty("created", model.created);
            return ret;
        }

        @Override
        public UsageDeltaStats toModel(Entity entity) {
            return new UsageDeltaStats(entity);
        }

        @Override
        public Class<UsageDeltaStats> getType() {
            return UsageDeltaStats.class;
        }

        @Override
        public Key toKey(UsageDeltaStats model) {
            return key(model.id);
        }
    };

}
