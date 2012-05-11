package models;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Objects;

public class Blacklist implements Serializable {
	
    private static final long serialVersionUID = 45L;

    private static final Mapper<Blacklist> MAPPER = new BlacklistMapper();

    private static final String KIND = "Blacklist";
    
    public Long id;
    public Date created;
    
    public Blacklist() {
        this.created = new Date();
    }
    
    public Blacklist(Long id) {
        this();
        this.id = id;
    }

    public Blacklist(Entity e) {
        this.id = e.getKey().getId();
        this.created = (Date) e.getProperty("created");
    }

    public void save() {
        DatastoreUtil.put(this, MAPPER);
    }

    public void delete() {
        DatastoreUtil.delete(this, MAPPER);
    }

    public static Blacklist findById(long id) {
        return DatastoreUtil.get(key(id), MAPPER);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.id)
            .append(this.created)
            .hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        Blacklist other = (Blacklist) obj;
        return new EqualsBuilder()
            .append(this.id, other.id)
            .append(this.created, other.created)
            .isEquals();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(Blacklist.class)
            .add("id", id)
            .add("created", created)
            .toString();
    }
    
    
    public static Key key(long id) {
        return KeyFactory.createKey(KIND, id);
    }
    
    public static Iterable<Blacklist> query(Query q, int limit) {
        FetchOptions fo;
        
        if (limit < 0) {
            fo = FetchOptions.Builder.withDefaults();
        } else {
            fo = FetchOptions.Builder.withLimit(limit);
        }
        
        return DatastoreUtil.query(q, fo, MAPPER);
    }

    public static Query all() {
        return new Query(KIND);
    }

    private static class BlacklistMapper implements Mapper<Blacklist> {
		
        @Override
        public Key getKey(Blacklist blacklist) {
        	return KeyFactory.createKey(KIND, blacklist.id);
        }

        @Override
        public Entity toEntity(Blacklist model) {
            Entity ret = new Entity(key(model.id));
            ret.setProperty("created", model.created);
            return ret;
        }

        @Override
        public Blacklist toModel(Entity entity) {
            return new Blacklist(entity);
        }

        @Override
        public Class<Blacklist> getType() {
            return Blacklist.class;
        }
    }
}
