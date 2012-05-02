package models;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.cache.Cache;
import play.modules.objectify.Datastore;
import play.modules.objectify.ObjectifyModel;

import com.google.common.base.Objects;
import com.google.gdata.util.common.base.Preconditions;
import com.googlecode.objectify.annotation.Cached;

import common.cache.CacheKey;

@Cached
public class Blacklist extends ObjectifyModel implements Serializable {

    // TODO: refactor this static block
    static {
        if ((Cache.cacheImpl != Cache.forcedCacheImpl) && (Cache.forcedCacheImpl != null)) {
            Logger.warn("Wrong cache impl, fixing. Cache manager: %s Forced manager: %s",
                        Cache.cacheImpl.getClass(),
                        Cache.forcedCacheImpl.getClass());
           Cache.cacheImpl = Cache.forcedCacheImpl;
        }
    }
    
    @Id public Long id;
    public Date created;
    
    public Blacklist() {
        this.created = new Date();
    }
    
    public Blacklist(Long id) {
        this();
        this.id = id;
    }

    public void save() {
        Datastore.put(this);
    }

    public void delete() {
        Datastore.delete(this);
    }

    public void invalidate() {
        Cache.safeDelete(CacheKey.create(Blacklist.class, id));
    }

    @PrePersist
    public void prePersist() {
        invalidate();
    }

    public static Blacklist findById(long id) {
        Preconditions.checkNotNull(id, "id cannot be null");
        String cacheKey = CacheKey.create(Blacklist.class, id);
        Blacklist blacklist = (Blacklist) Cache.get(cacheKey);
        if (blacklist == null) {
            blacklist = Datastore.find(Blacklist.class, id, false);
            if (blacklist != null) {
              Cache.set(cacheKey, blacklist, "15mn");
            }
        }
        return blacklist;
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
        if (!super.equals(obj))
            return false;
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
}
