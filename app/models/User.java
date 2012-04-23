package models;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.repackaged.com.google.common.base.Throwables;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.gdata.util.common.base.Preconditions;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;

import dropbox.gson.DbxAccount;

import rules.RuleType;

public class User implements Serializable {

    static {
        if ((Cache.cacheImpl != Cache.forcedCacheImpl) && (Cache.forcedCacheImpl != null)) {
            Logger.warn("Wrong cache impl, fixing. Cache manager: %s Forced manager: %s",
                        Cache.cacheImpl.getClass(),
                        Cache.forcedCacheImpl.getClass());
           Cache.cacheImpl = Cache.forcedCacheImpl;
        }
    }

    public static final Function<Entity, User> TO_USER = new Function<Entity, User>() {
        @Override public User apply(Entity entity) {
            return new User(entity);
        }};

    private static String getCacheKey(Long id) {
        return String.format("user:%d", id);
    }

    public static final String KIND = "User";

    public Long id;
    public String name;
    public String email;
    public Boolean periodicSort;
    public Date created;
    public Date modified;
    public Date lastSync;

    private String token;
    private String secret;
    
    public User() {
        this.created = this.modified = new Date();
        this.periodicSort = true;
    }

    public User(DbxAccount account, String token, String secret) {
        this();
        this.id = account.uid;
        this.name = account.name;
        setToken(token);
        setSecret(secret);
    }
    
    public User(Entity entity) {
        this.id = (Long) entity.getProperty("id");
        this.name = (String) entity.getProperty("name");
        this.email = (String) entity.getProperty("email");
        this.periodicSort = (Boolean) entity.getProperty("periodicSort");
        this.created = (Date) entity.getProperty("created");
        this.modified = (Date) entity.getProperty("modified");
        this.lastSync = (Date) entity.getProperty("lastSync");
        
        this.token = (String) entity.getProperty("token");
        this.secret = (String) entity.getProperty("secret");
    }
    
    public Entity toEntity() {
        Entity entity = new Entity(getKey());
        entity.setProperty("id", id);
        entity.setProperty("name", name);
        entity.setProperty("email", email);
        entity.setProperty("periodicSort", periodicSort);
        entity.setProperty("created", created);
        entity.setProperty("modified", modified);
        entity.setProperty("lastSync", lastSync);
        
        entity.setProperty("token", token);
        entity.setProperty("secret", secret);
        return entity;
    }

    public String getKind() {
        return KIND;
    }

    public Key getKey() {
        return KeyFactory.createKey(KIND, id);
    }
    
    /**
     * Only for testing.
     */
    public void setTokenRaw(String token) {
        assert Play.runingInTestMode();
        this.token = token;
    }
    
    /**
     * Only for testing.
     */
    public void setSecretRaw(String secret) {
        assert Play.runingInTestMode();
        this.secret = secret;
    }

    public void setToken(String token) {
        this.token = Crypto.encryptAES(token);
    }
    
    public void setSecret(String secret) {
        this.secret = Crypto.encryptAES(secret);
    }
    
    public String getToken() {
        try {
            return Crypto.decryptAES(this.token);
        } catch (UnexpectedException e) {
            String tmp = this.token;
            setToken(this.token);
            return tmp;
        }
    }
    
    public String getSecret() {
        try {
            return Crypto.decryptAES(this.secret);
        } catch (UnexpectedException e) {
            String tmp = this.secret;
            setSecret(this.secret);
            return tmp;
        }
    }

    public static User findById(Long id) {
        Preconditions.checkNotNull(id, "id cannot be null");
        String cacheKey = getCacheKey(id);
        User user = (User) Cache.get(cacheKey);
        if (user == null) {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            try {
                Key key = KeyFactory.createKey(KIND, id);
                user = new User(ds.get(key));
                Cache.add(cacheKey, user, "1h");
            } catch (EntityNotFoundException e) {
                return null;
            }
        }
        return user;
    }
    
    public static User findOrCreateByDbxAccount(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull())
            return null;
        
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        Key key = KeyFactory.createKey(KIND, account.uid);

        Transaction tx = ds.beginTransaction();
        try {
            Entity entity = null;
            User user = null;
            try {
                String cacheKey = getCacheKey(account.uid);
                user = (User) Cache.get(cacheKey);
                if (user == null) {
                    entity = ds.get(key);
                    user = new User(entity);
                }
                if (!user.getToken().equals(token) || !user.getSecret().equals(secret)){
                    // TODO: update other fields if stale
                    user.setToken(token);
                    user.setSecret(secret);
                    user.modified = new Date();
                }
            } catch (EntityNotFoundException e) {
                user = new User(account, token, secret);
                Logger.info("Created user: %s", user);
            }
            user.invalidate();
            entity = user.toEntity();
            ds.put(tx, entity);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        } 

        try {
            return new User(ds.get(key));
        } catch (EntityNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void update(User user) {
        Preconditions.checkNotNull(user, "user can't be null");
        // update the modified date
        user.modified = new Date();
        user.invalidate();
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        ds.put(user.toEntity());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.id)
            .append(this.name)
            .append(this.secret)
            .append(this.token)
            .append(this.email)
            .append(this.periodicSort)
            .append(this.created)
            .append(this.modified)
            .append(this.lastSync)
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
        User other = (User) obj;
        return new EqualsBuilder()
            .append(this.id, other.id)
            .append(this.name, other.name)
            .append(this.secret, other.secret)
            .append(this.token, other.token)
            .append(this.email, other.email)
            .append(this.periodicSort, other.periodicSort)
            .append(this.created, other.created)
            .append(this.modified, other.modified)
            .append(this.lastSync, other.lastSync)
            .isEquals();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(User.class)
            .add("id", id)
            .add("name", name)
            .add("email", email)
            .add("periodicSort", periodicSort)
            .add("created_date", created)
            .add("last_update", modified)
            .add("last_sync", lastSync)
            .toString();
    }
    
    /**
     * Invalidate the cached version of this object.
     */
    public void invalidate() {
        Cache.safeDelete(getCacheKey(this.id));
    }
}
