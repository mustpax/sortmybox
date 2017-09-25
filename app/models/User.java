package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.persistence.Cacheable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import com.dropbox.core.DbxAuthFinish;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import box.BoxAccount;
import box.BoxCredentials;
import dropbox.Dropbox;
import dropbox.gson.DbxAccount;
import play.Logger;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Crypto;

@Cacheable
public class User implements Serializable {
    private static final String KEY_DELIM = ":";

    public static enum AccountType {
        DROPBOX,
        BOX;
        
	    public static AccountType fromDbValue(String dbValue) {
	        for (AccountType type : AccountType.values()) {
	            if (type.name().equals(dbValue)) return type;
	        }

	        return null;
	    }
    }
	
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
    public String sortingFolder;

    public Date created;
    public Date modified;
    public Date lastSync;
    public Date lastLogin;

    public AccountType accountType;
    
    private String token;
    private String secret;
    public String dropboxV2Id;
    public String dropboxV2Token;
    public boolean dropboxV2Migrated = false;

    // Used by Box client only
    private String refreshToken;

    private Date tokenExpiration;
    
    
    public User(AccountType at) {
        this.modified = this.created = this.lastLogin = new Date();
        this.periodicSort = true;
        this.fileMoves = 0;
        this.accountType = at;
        this.sortingFolder = Dropbox.getSortboxPath();
    }

    public User(DbxAccount account, String token, String secret) {
        this(AccountType.DROPBOX);
        this.id = account.uid;
        sync(account, token, secret);
    }
    
    public User(Entity entity) {
        this.name = (String) entity.getProperty("name");
        this.nameLower = (String) entity.getProperty("nameLower");
        if (this.nameLower == null && this.name != null) {
            this.nameLower = this.name.toLowerCase();
        }
        this.email = (String) entity.getProperty("email");
        this.periodicSort = (Boolean) entity.getProperty("periodicSort");
        this.created = (Date) entity.getProperty("created");
        this.modified = (Date) entity.getProperty("modified");
        this.lastSync = (Date) entity.getProperty("lastSync");
        this.lastLogin = (Date) entity.getProperty("lastLogin");
        this.token = (String) entity.getProperty("token");
        this.secret = (String) entity.getProperty("secret");
        Long tmpFileMoves = (Long) entity.getProperty("fileMoves");
        this.fileMoves = tmpFileMoves == null ? 0 : tmpFileMoves.intValue();
        this.sortingFolder = (String) entity.getProperty("sortingFolder");
        if (this.sortingFolder == null) {
            this.sortingFolder = Dropbox.getOldSortboxPath();
        }
        this.tokenExpiration = (Date) entity.getProperty("tokenExpiration");
        this.refreshToken = (String) entity.getProperty("refreshToken");
        this.dropboxV2Token = (String) entity.getProperty("dropboxV2Token");
        this.dropboxV2Id = (String) entity.getProperty("dropboxV2Id");
        this.dropboxV2Migrated = (Boolean) entity.getProperty("dropboxV2Migrated") == Boolean.TRUE;

        this.accountType = AccountType.fromDbValue((String) entity.getProperty("accountType"));
        if (this.accountType == null) {
            this.accountType = AccountType.DROPBOX;
        }

        switch (this.accountType) {
        case DROPBOX:
            this.id = entity.getKey().getId();
            break;
        case BOX:
            this.id = Long.valueOf(entity.getKey().getName().split(KEY_DELIM)[1]);
            break;
        }
    }

    public User(DbxAccount acc, DbxAuthFinish auth) {
        this(AccountType.DROPBOX);
        this.dropboxV2Migrated = true;
        this.dropboxV2Token = auth.getAccessToken();
        this.dropboxV2Id = auth.getUserId();
        this.name = acc.name;
        this.email = acc.email;
    }

    private static Set<Long> getAdmins() {
        if (Play.configuration == null) {
            return Collections.emptySet();
        }
        
        String ids = Play.configuration.getProperty("sortbox.admins", "");
        if (ids == null) {
            ids = "";
        } else {
            ids = ids.trim();
        }
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

    public void setRefreshToken(String token) {
        this.refreshToken = Crypto.encryptAES(token);
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

    public Key getKey() {
        if (this.id == null) {
            return null;
        }
        return User.key(this.accountType, this.id);
    }

    public boolean isAdmin() {
        return accountType == AccountType.DROPBOX && ADMINS.contains(id);
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
    public void sync(DbxAccount account, String token, String secret) {
        setName(account.name);
        setToken(token);
        setSecret(secret);
    }

    public Key save() {
        this.modified = new Date();
        Key ret = DatastoreUtil.put(this, MAPPER);
        if (this.id == null) {
            this.id = ret.getId();
        }
        return ret;
    }

    public void delete() {
        DatastoreUtil.delete(this, MAPPER);
    }

    /**
     * @param accountType account type of the user
     * @param id the user id
     * @return fully loaded user for the given id, null if not found.
     */
    public static User findById(AccountType accountType, long id) {
        return findByKey(key(accountType, id));
    }

    public static User findByKey(Key key) {
        return DatastoreUtil.get(key, MAPPER);
    }

    public static Key key(AccountType accountType, long id) {
        // Handle null AccountTypes gracefully for tests.
        if (accountType == null && Play.runingInTestMode()) {
            accountType = AccountType.DROPBOX;
        }

        switch (accountType) {
        case BOX:
            String strId = AccountType.BOX.name() + KEY_DELIM + id;
            return KeyFactory.createKey(KIND, strId);
        case DROPBOX:
            return KeyFactory.createKey(KIND, id);
        }

        throw new IllegalStateException("Cannot create User key for AccountType: " + accountType);
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

    /**
     * Upsert a Box user into the datastore.
     */
    public static User upsert(BoxCredentials cred, BoxAccount account) {
        if (account == null) {
            return null;
        }

        User user = findById(AccountType.BOX, account.id);
        if (user == null) {
            user = new User(AccountType.BOX);
            user.id = account.id;
            Logger.info("Box user not found in datastore, creating new one: %s", user);
        }

        user.setToken(cred.token);
        user.setRefreshToken(cred.refeshToken);
        user.email = account.email;
        // TODO handle null name
        user.name = account.name;
        DateTime expiration = DateTime.now().plusSeconds(cred.expiresIn);
        user.lastLogin = new Date();
        user.tokenExpiration = expiration.toDate();
        user.save();
        
        return user;
    }

    public static User upsert(DbxAccount account, DbxAuthFinish auth) {
        User u = DatastoreUtil.get(User.all().addFilter("dropboxV2Id", FilterOperator.EQUAL, auth.getUserId()), User.MAPPER);
        if (u == null) {
            u = new User(account, auth);
            Logger.info("Dropbox user not found in datastore, creating new one: %s", u);
        } else {
            Logger.info("Updating Dropbox credentials for user: %s", u);
            u.dropboxV2Token = auth.getAccessToken();
        }
        u.lastLogin = new Date();
        u.save();
        return u;
    }

    public static User upsert(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull()) {
            return null;
        }
    
        User user = findById(AccountType.DROPBOX, account.uid);
        if (user == null) {
            user = new User(account, token, secret);
            Logger.info("Dropbox user not found in datastore, creating new one: %s", user);
        } else {
            if (! user.equals(account, token, secret)) {
                Logger.info("User has new Dropbox oauth credentials: %s", user);
                user.sync(account, token, secret);
            }
            
            user.lastLogin = new Date();
        }

        user.save();
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
            .append(this.sortingFolder)
            .append(this.accountType)
            .append(this.refreshToken)
            .append(this.dropboxV2Token)
            .append(this.dropboxV2Migrated)
            .append(this.dropboxV2Id)
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
            .append(this.sortingFolder, other.sortingFolder)
            .append(this.accountType, other.accountType)
            .append(this.refreshToken, other.refreshToken)
            .append(this.dropboxV2Token, other.dropboxV2Token)
            .append(this.dropboxV2Migrated, other.dropboxV2Migrated)
            .append(this.dropboxV2Id, other.dropboxV2Id)
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
        return MoreObjects.toStringHelper(User.class)
            .add("id", id)
            .add("dropboxV2Id", dropboxV2Id)
            .add("accountType", accountType)
            .add("name", name)
            .add("nameLower", nameLower)
            .add("email", email)
            .add("periodicSort", periodicSort)
            .add("dropboxV2Migrated", dropboxV2Migrated)
            .add("created_date", created)
            .add("last_update", modified)
            .add("last_sync", lastSync)
            .add("last_login", lastLogin)
            .add("sortingFolder", sortingFolder)
            .toString();
    }

    private static class UserMapper implements Mapper<User> {
        @Override
        public Entity toEntity(User model) {
            Entity entity;
            if (model.id == null) {
                entity = new Entity(KIND);
            } else {
                entity = new Entity(toKey(model));
            }

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
            entity.setProperty("sortingFolder", model.sortingFolder);
            if (model.accountType != null) {
                entity.setProperty("accountType", model.accountType.name());
            }
            entity.setProperty("tokenExpiration", model.tokenExpiration);
            entity.setProperty("refreshToken", model.refreshToken);
            entity.setProperty("dropboxV2Token", model.dropboxV2Token);
            entity.setProperty("dropboxV2Migrated", model.dropboxV2Migrated);
            entity.setProperty("dropboxV2Id", model.dropboxV2Id);
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
            return model.getKey();
        }
    }

}