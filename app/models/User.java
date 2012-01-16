package models;

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
    
    public Key getKey() {
        return this.entity.getKey();
    }
    
    public static User getOrCreate(DbxUser u) {
        if ((u == null) ||
            ! u.notNull()) {
            return null;
        }
        Key k = KeyFactory.createKey("user", u.uid);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Entity e;
        try {
            e = datastore.get(k);
        } catch (EntityNotFoundException e1) {
            e = new Entity(k);
            e.setProperty("uid", u.uid);
            datastore.put(e);
        }

        return new User(e);
    }
}
