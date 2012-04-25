package models;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import play.modules.objectify.Datastore;
import play.modules.objectify.ObjectifyModel;

import com.google.common.base.Objects;
import com.google.gdata.util.common.base.Preconditions;
import com.googlecode.objectify.Key;

import dropbox.gson.DbxAccount;

public class User extends ObjectifyModel implements Serializable {

    static {
        if ((Cache.cacheImpl != Cache.forcedCacheImpl) && (Cache.forcedCacheImpl != null)) {
            Logger.warn("Wrong cache impl, fixing. Cache manager: %s Forced manager: %s",
                        Cache.cacheImpl.getClass(),
                        Cache.forcedCacheImpl.getClass());
           Cache.cacheImpl = Cache.forcedCacheImpl;
        }
    }

    private static String getCacheKey(Long id) {
        return String.format("user:%d", id);
    }

    @Id public Long id;
    public String name;
    public String email;
    public Boolean periodicSort;
    public Date created;
    public Date modified;
    public Date lastSync;

    private String token;
    private String secret;
    
    public User() {
        this.created = new Date();
        this.periodicSort = true;
    }

    public User(DbxAccount account, String token, String secret) {
        this();
        this.id = account.uid;
        this.name = account.name;
        setToken(token);
        setSecret(secret);
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

    /**
     * @param id the user id
     * @return fully loaded user for the given id, null if nout found.
     */
    public static User findById(long id) {
        Preconditions.checkNotNull(id, "id cannot be null");
        String cacheKey = getCacheKey(id);
        User user = (User) Cache.get(cacheKey);
        if (user == null) {
            user = Datastore.find(User.class, id, false);
            if (user != null) {
              Cache.set(cacheKey, user, "1h");
            }
        }
        return user;
    }

    public static User getOrCreateUser(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull()) {
            return null;
        }

        User user = findById(account.uid);
        if (user == null) {
            user = new User(account, token, secret);
            Logger.info("Dropbox user not found in datastore, creating new one: %s", user);
            user.save();
        }

        if (!user.getToken().equals(token) || !user.getSecret().equals(secret)){
            // TODO: update other fields if stale
            Logger.info("User has new Dropbox oauth credentials: %s", user);
            user.setToken(token);
            user.setSecret(secret);
            user.save();
        }
        
        return user;
    }

    public void updateLastSyncDate() {
        lastSync = new Date();
        save();
    }
    
    public Key<User> save() {
        this.invalidate();
        return Datastore.put(this);
    }

    @PrePersist
    public void prePersist() {
        modified = new Date();
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
