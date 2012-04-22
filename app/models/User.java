package models;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;
import siena.Column;
import siena.DateTime;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.NotNull;
import siena.Query;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.FileMoveCollisionException;
import dropbox.gson.DbxAccount;
import dropbox.gson.DbxMetadata;

/**
 * Model for a user.
 * 
 * @author mustpax
 * @author syyang
 */
public class User extends Model implements Serializable {
    private static AtomicBoolean cacheInit = new AtomicBoolean(false);

    private static String getCacheKey(Long id) {
        return String.format("user:%d", id);
    }
    
    // the id will be explicitly set to Dropbox uid
    @Id(Generator.NONE)
    public Long id;
    
    @NotNull
    private String token;
    
    @NotNull
    private String secret;
    
    public String email;

    public String name;

    public Integer hash;

    @DateTime
    public Date created;

    @DateTime
    public Date modified;
    
    @DateTime
    @Column("last_sync")
    public Date lastSync;

    public User() {
        this.created = this.modified = new Date();
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
     * Process all rules for the current user and move files to new location
     * as approriate.
     * 
     * @return list of file moves performed
     */
    public List<FileMove> runRules() {
        List<FileMove> ret = Lists.newArrayList();
        DropboxClient client = DropboxClientFactory.create(this);
        Set<String> files = client.listDir(Dropbox.getRoot().getSortboxPath());

        if (files.isEmpty()) {
            Logger.info("Ran rules for %s, no files to process.", this);
            return ret;
        }

        List<Rule> rules = Rule.findByOwner(this).fetch();
        Logger.info("Running rules for %s", this);
        
        for (String file: files) {
            String base = basename(file);
            for (Rule r: rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule id: %s", file, r.dest, r.id);
                    boolean success = true;
                    try {
                        String dest = r.dest +
                                      (r.dest.endsWith("/") ? "" : "/") +
                                      base;
                        client.move(file, dest);
                    } catch (FileMoveCollisionException e) {
                        success = false;
                    }
                    ret.add(new FileMove(r, base, success));
                    break;
                }
            }
        }

        Logger.info("Done running rules for %s. %d moves performed", this, ret.size());
        if (! ret.isEmpty()) {
            Model.batch(FileMove.class).insert(ret);
        }

        // Delete old Move rows with 1% probability
        if ((new Random().nextInt() % 100) == 0) {
            FileMove.deleteStaleForUser(this.id);
        }
        
        return ret;
    }
    
    public Query<FileMove> getMoves() {
        return FileMove.all().filter("owner", this.id).order("-when");
    }

    /**
     * Create the Sortbox folder for this user if necessary. 
     * @return true if a sortbox folder was created, false if nothing was done
     */
    public boolean createSortboxIfNecessary() {
        DropboxClient client = DropboxClientFactory.create(this);
        String sortboxPath = Dropbox.getRoot().getSortboxPath();
        DbxMetadata file = client.getMetadata(sortboxPath);

        if (file == null) {
            Logger.info("Sortbox folder missing for user '%s' at path '%s'", this, sortboxPath);
            return client.mkdir(sortboxPath) != null;
        }

        return false;
    }

    private static String basename(String path) {
        if (path == null) {
            return null;
        }
        
        File f = new File(path);
        return f.getName();
    }
    
    public static Query<User> all() {
        return Model.all(User.class);
    }
    
    public static User findById(Long id) {
        assert id != null : "id cannot be null";

        // Play sometimes does not switch to the forcedCacheImpl so
        // we switch it ourselves
        if (cacheInit.compareAndSet(false, true)) {
            if ((Cache.cacheImpl != Cache.forcedCacheImpl) &&
                (Cache.forcedCacheImpl != null)) {
                Logger.warn("Wrong cache impl, fixing. Cache manager: %s Forced manager: %s",
                            Cache.cacheImpl.getClass(),
                            Cache.forcedCacheImpl.getClass());
                Cache.cacheImpl = Cache.forcedCacheImpl;
            }
        }

        String key = getCacheKey(id);
        User ret = (User) Cache.get(key);
        if (ret == null) {
	        ret = all().filter("id", id).get();
	        if (ret != null) {
	            Cache.add(key, ret, "1h");
	        }
        }
        return ret;
    }
    
    public static User findOrCreateByDbxAccount(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull())
            return null;
        User user = findById(account.uid);
        if (user == null) {
            user = new User(account, token, secret);
            user.insert();
        } else if (!user.getToken().equals(token) || !user.getSecret().equals(secret)){
            // TODO: update other fields if stale
            user.setToken(token);
            user.setSecret(secret);
            user.modified = new Date();
            user.update();
        }
        return user;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.id)
            .append(this.name)
            .append(this.secret)
            .append(this.token)
            .append(this.email)
            .append(this.hash)
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
            .append(this.hash, other.hash)
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
            .add("hash", hash)            
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

    @Override
    public void delete() {
        invalidate();
        super.delete();
    }

    @Override
    public void save() {
        invalidate();
        super.save();
    }

    @Override
    public void update() {
        invalidate();
        super.update();
    }
    
}
