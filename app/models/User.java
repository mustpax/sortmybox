package models;

import java.util.Arrays;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.google.common.base.Objects;

import siena.Generator;
import siena.Id;
import siena.Model;
import siena.NotNull;
import siena.Query;

import dropbox.gson.DbxUser;

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

    public User() { }

    public User(DbxUser dbxUser, String token, String secret) {
        this(dbxUser.uid, dbxUser.name, token, secret, null);
    }

    public User(Long id, String name, String token, String secret, String email) {
        this.id = id;
        this.name = name;
        this.token = token;
        this.secret = secret;
        this.email = email;
    }
    
    public static Query<User> all() {
        return Model.all(User.class);
    }
    
    public static User findById(Long id) {
        return all().filter("id", id).get();
    }
    
    public static User findOrCreateByDbxUser(DbxUser dbxUser, String token, String secret) {
        if (dbxUser == null || !dbxUser.notNull())
            return null;
        User user = findById(dbxUser.uid);
        if (user == null) {
            user = new User(dbxUser, token, secret);
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
        HashCodeBuilder hash = new HashCodeBuilder()
            .append(this.id)
            .append(this.name)
            .append(this.secret)
            .append(this.token)
            .append(this.email);
        return hash.hashCode();
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
        EqualsBuilder eq = new EqualsBuilder()
            .append(this.id, other.id)
            .append(this.name, other.name)
            .append(this.secret, other.secret)
            .append(this.token, other.token)
            .append(this.email, other.email);
        return eq.isEquals();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(User.class)
            .add("id", id)
            .add("name", name)
            .add("email", email)        
            .toString();
    }
}
