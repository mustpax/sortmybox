package models;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Objects;

import siena.Column;
import siena.DateTime;
import siena.Generator;
import siena.Id;
import siena.Model;
import siena.NotNull;
import siena.Query;

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

    public User() { }

    public User(DbxAccount account, String token, String secret) {
        this.id = account.uid;
        this.name = account.name;
        this.token = token;
        this.secret = secret;
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
