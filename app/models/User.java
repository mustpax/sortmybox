package models;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import play.modules.objectify.Datastore;
import play.modules.objectify.ObjectifyModel;

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableSet;
import com.google.common.base.Objects;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cached;

import dropbox.gson.DbxAccount;

@Cached
public class User extends ObjectifyModel implements Serializable {

    private static final Set<Long> ADMINS = getAdmins();

    public static final String KIND = "User";

    @Id public Long id;
    public String name;
    public String nameLower;
    public String email;
    public Boolean periodicSort;
    public Integer fileMoves;

    public Date created;
    public Date modified;
    public Date lastSync;
    public Date lastLogin;

    private String token;
    private String secret;
    
    public User() {
        this.created = this.lastLogin = new Date();
        this.periodicSort = true;
        this.fileMoves = 0;
    }

    public User(DbxAccount account, String token, String secret) {
        this();
        this.id = account.uid;
        this.name = account.name;
        this.nameLower = name.toLowerCase();
        setToken(token);
        setSecret(secret);
    }
    
    private static Set<Long> getAdmins() {
        String ids= Play.configuration.getProperty("sortbox.admins", "").trim();
        ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
        for (String id : ids.split(",")) {
            if (id.isEmpty()) continue;
            builder.add(Long.valueOf(id));
        }
        return builder.build();
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

    public boolean isAdmin() {
        return ADMINS.contains(id);
    }

    public static boolean isValidId(String userId) {
        // check input is not null and not empty
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        // check input is a valid user id
        try {
            Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return false;
        }
        
        return true;
    }
    
    /**
     * @param id the user id
     * @return fully loaded user for the given id, null if not found.
     */
    public static User findById(long id) {
        try {
            Key<User> key = Datastore.key(User.class, id);
            return Datastore.get(key);
        } catch (EntityNotFoundException e) {
            return null;
        } catch (IllegalArgumentException e) {
            Logger.warn("Failed to get user: %d", id);
            throw e;
        }
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
        } else {
            if (!user.getToken().equals(token) || !user.getSecret().equals(secret)){
                // TODO: update other fields if stale
                Logger.info("User has new Dropbox oauth credentials: %s", user);
                user.setToken(token);
                user.setSecret(secret);
            }

            if (user.nameLower == null) {
                user.nameLower = user.name.toLowerCase();
            }

            if (user.fileMoves == null) {
                user.fileMoves = 0;
            }
            
            user.lastLogin = new Date();

            user.save();
        }

        return user;
    }

    public void updateLastSyncDate() {
        lastSync = new Date();
        save();
    }

    public void incrementFileMoves(int count) {
        fileMoves += count;
        save();
    }

    public Key<User> save() {
        return Datastore.put(this);
    }

    public void delete() {
        Datastore.delete(this);
    }
    
    public static com.google.appengine.api.datastore.Key key(long id) {
        return KeyFactory.createKey(KIND, id);
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
            .append(this.nameLower)
            .append(this.secret)
            .append(this.token)
            .append(this.email)
            .append(this.periodicSort)
            .append(this.created)
            .append(this.modified)
            .append(this.lastSync)
            .append(this.lastLogin)
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
            .append(this.nameLower, other.nameLower)            
            .append(this.secret, other.secret)
            .append(this.token, other.token)
            .append(this.email, other.email)
            .append(this.periodicSort, other.periodicSort)
            .append(this.created, other.created)
            .append(this.modified, other.modified)
            .append(this.lastSync, other.lastSync)
            .append(this.lastLogin, other.lastLogin)
            .isEquals();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(User.class)
            .add("id", id)
            .add("name", name)
            .add("nameLower", nameLower)
            .add("email", email)
            .add("periodicSort", periodicSort)
            .add("created_date", created)
            .add("last_update", modified)
            .add("last_sync", lastSync)
            .add("last_login", lastLogin)
            .toString();
    }

}
