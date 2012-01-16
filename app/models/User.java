package models;

import java.util.Arrays;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import dropbox.gson.DbxUser;

public class User {
    private Entity entity;
    
    private User(Entity e) {
        this.entity = e;
    }

    public long getUId() {
        return (Long) this.entity.getProperty("uid");
    }
    
    public String getToken() {
        return (String) this.entity.getProperty("token");
    }

    public String getSecret() {
        return (String) this.entity.getProperty("secret");
    }

    public String getEmail() {
        return (String) this.entity.getProperty("email");
    }

    public String getName() {
        return (String) this.entity.getProperty("name");
    }

    public Key getKey() {
        return this.entity.getKey();
    }
    
    public static User getOrCreate(DbxUser u, String token, String secret) {
        if ((u == null) ||
            ! u.notNull()) {
            return null;
        }
        Key k = KeyFactory.createKey("user", u.uid);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Entity e;
        try {
            e = datastore.get(k);

            // TODO update other fields if stale
            User tmp = new User(e);
            if ((! token.equals(tmp.getToken())) ||
                (! secret.equals(tmp.getSecret()))) {
                e.setProperty("token", token);
                e.setProperty("secret", secret);
                datastore.put(e);
            }
        } catch (EntityNotFoundException e1) {
            e = new Entity(k);
            e.setProperty("uid", u.uid);
            e.setProperty("token", token);
            e.setProperty("secret", secret);
            e.setProperty("email", u.email);
            e.setProperty("name", u.name);
            datastore.put(e);
        }

        return new User(e);
    }
    
    
}
