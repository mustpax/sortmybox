package models;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import play.Logger;
import play.Play;
import siena.Column;
import siena.DateTime;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.Query;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.FileMoveCollisionException;
import dropbox.gson.DbxAccount;

/**
 * Model for a user.
 * 
 * @author mustpax
 * @author syyang
 */
public class User extends Model {
    
    // the id will be explicitly set to Dropbox uid
    @Id(Generator.NONE)
    public Long id;
    
    public String token;
    
    public String secret;
    
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
        this.created = new Date();
        this.modified = new Date();
    }

    public User(DbxAccount account, String token, String secret) {
        this.id = account.uid;
        this.name = account.name;
        this.token = token;
        this.secret = secret;
        this.created = new Date();
        this.modified = new Date();
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
        List<Rule> rules = Rule.findByOwner(this).fetch();
        Logger.info("Running rules for %s", this);
        
        for (String file: files) {
            String base = basename(file);
            for (Rule r: rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule id: %s", file, r.dest, r.id);
                    boolean success = true;
                    // TODO do not move files on production just yet
                    if (Play.mode.isDev()) {
                        try {
                            client.move(file, r.dest + "/" + base);
                        } catch (FileMoveCollisionException e) {
                            success = false;
                        }
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
        return all().filter("id", id).get();
    }
    
    public static User findOrCreateByDbxAccount(DbxAccount account, String token, String secret) {
        if (account == null || !account.notNull())
            return null;
        User user = findById(account.uid);
        if (user == null) {
            user = new User(account, token, secret);
            user.insert();
        } else if (!user.token.equals(token) || !user.secret.equals(secret)){
            // TODO: update other fields if stale
            user.token = token;
            user.secret = secret;
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
}
