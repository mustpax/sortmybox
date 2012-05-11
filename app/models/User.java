package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.persistence.Cacheable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableSet;
import com.google.common.base.Objects;

import dropbox.gson.DbxAccount;

@Cacheable
public class User implements Serializable {
	
    private static final long serialVersionUID = 45L;
    
    public static final Mapper<User> MAPPER = new UserMapper();

    private static final Set<Long> ADMINS = getAdmins();

    public static final String KIND = "User";

    public Long id;
    private String name;
    private String nameLower;
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
        this.modified = this.created = this.lastLogin = new Date();
        this.periodicSort = true;
        this.fileMoves = 0;
    }

    public User(DbxAccount account, String token, String secret) {
        this();
        this.id = account.uid;
        sync(account, token, secret);
    }
    
    public User(Entity entity) {
        this.id = entity.getKey().getId();
        this.name = (String) entity.getProperty("name");
        this.nameLower = (String) entity.getProperty("nameLower");
        this.email = (String) entity.getProperty("email");
        this.periodicSort = (Boolean) entity.getProperty("periodicSort");
        this.created = (Date) entity.getProperty("created");
        this.modified = (Date) entity.getProperty("modified");
        this.lastSync = (Date) entity.getProperty("lastSync");
        this.lastLogin = (Date) entity.getProperty("lastLogin");
        this.token = (String) entity.getProperty("token");
        this.secret = (String) entity.getProperty("secret");
        this.fileMoves = ((Long) entity.getProperty("fileMoves")).intValue();
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

    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
        this.nameLower = name.toLowerCase();
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

    public void updateLastSyncDate() {
        lastSync = new Date();
        save();
    }

    public void incrementFileMoves(int count) {
        fileMoves += count;
        save();
    }
    
    /**
     * Update this user with Dropbox credentials
     */
    public void sync(DbxAccount account, String secret, String token) {
        setName(account.name);
        setSecret(secret);
        setToken(token);
    }

    public Key save() {
        this.modified = new Date();
        return DatastoreUtil.put(this, MAPPER);
    }

    public void delete() {
        DatastoreUtil.delete(this, MAPPER);
    }
    
    /**
     * @param id the user id
     * @return fully loaded user for the given id, null if not found.
     */
    public static User findById(long id) {
        return DatastoreUtil.get(key(id), MAPPER);
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

    public static Key key(long id) {
        return KeyFactory.createKey(KIND, id);
    }

    public static Query all() {
        return new Query(KIND);
    }

    public static Iterable<User> query(Query q) {
        return query(q, -1);
    }

    public static Iterable<User> query(Query q, int limit) {
        assert q.getKind().equals(KIND) : "Query kind must be User";
        FetchOptions fo;
        if (limit < 0) {
            fo = FetchOptions.Builder.withDefaults();
        } else {
            fo = FetchOptions.Builder.withLimit(limit);
        }

        return DatastoreUtil.query(q, fo, MAPPER);
    }

    public static Iterable<Key> queryKeys(Query q) {
        assert q.getKind().equals(KIND) : "Query kind must be User";
        return DatastoreUtil.queryKeys(q, FetchOptions.Builder.withDefaults(), MAPPER);
    }

    public static User upsert(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull()) {
            return null;
        }
    
        User user = findById(account.uid);
        if (user == null) {
            user = new User(account, token, secret);
            Logger.info("Dropbox user not found in datastore, creating new one: %s", user);
            user.save();
        } else {
            if (! user.equals(account, token, secret)) {
                Logger.info("User has new Dropbox oauth credentials: %s", user);
                user.sync(account, token, secret);
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
    
    public boolean equals(DbxAccount account, String secret, String token) {
        return new EqualsBuilder()
            .append(this.name, account.name)
            .append(this.getSecret(), secret)
            .append(this.getToken(), token)
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

    private static class UserMapper implements Mapper<User> {

        @Override
        public Key getKey(User user) {
            return KeyFactory.createKey(KIND, user.id);
        }

        @Override
        public Entity toEntity(User model) {
            Entity entity = new Entity(key(model.id));
            entity.setProperty("name", model.name);
            entity.setProperty("nameLower", model.nameLower);
            entity.setProperty("email", model.email);
            entity.setProperty("periodicSort", model.periodicSort);
            entity.setProperty("created", model.created);
            entity.setProperty("modified", model.modified);
            entity.setProperty("lastSync", model.lastSync);
            entity.setProperty("token", model.token);
            entity.setProperty("secret", model.secret);
            entity.setProperty("fileMoves", model.fileMoves);
            entity.setProperty("lastLogin", model.lastLogin);
            return entity;
        }

        @Override
        public User toModel(Entity entity) {
            return new User(entity);
        }

        @Override
        public Class<User> getType() {
            return User.class;
        }

        @Override
        public Key toKey(User model) {
            return key(model.id);
        }
    }
}
