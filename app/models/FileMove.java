package models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Id;

import org.joda.time.DateTime;

import play.data.validation.Required;
import play.modules.objectify.Datastore;
import play.modules.objectify.ObjectifyModel;

import com.google.gdata.util.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.annotation.Parent;

public class FileMove extends ObjectifyModel implements Serializable {

    public static final int RETENTION_DAYS = 7;
    
    @Id public Long id;
    @Required @Parent public Key<User> owner;
    public String fromFile;
    public String toDir;
    public Date when;
    public Boolean successful;
    
    public FileMove(Long owner, Rule rule, String from, boolean success) {
        this.owner = Datastore.key(User.class, owner);
        this.toDir = rule.dest;
        this.fromFile = from;
        this.when = new Date();
        this.successful = success;
    }
    
    public boolean isSuccessful() {
        return this.successful == null ? true : this.successful;
    }

    @Override
    public String toString() {
        if (isSuccessful()) {
            return String.format("Moved file '%s' to '%s' at %s",
			                     this.fromFile, this.toDir, this.when);
        }

        return String.format("Failed to move file '%s' to '%s' at %s",
			                 this.fromFile, this.toDir, this.when);
    }

    public static FileMove findById(Long userId, Long fileMoveId) {
        Key<FileMove> fileMoveKey = Datastore.key(User.class, userId, FileMove.class, fileMoveId);
        return Datastore.find(fileMoveKey, false);
    }

    public static Query<FileMove> findByUser(User user) {
        Preconditions.checkNotNull(user, "User can't be null");
        return Datastore.query(FileMove.class)
                .ancestor(Datastore.key(User.class, user.id))
                .filter("when >", user.created)
                .order("-when");
    }

    public static void save(List<FileMove> fileMoves) {
        Objectify ofy = Datastore.beginTxn();
        try {
            Datastore.put(fileMoves);
            Datastore.commit();
        } finally {
            if (ofy.getTxn().isActive()) {
                ofy.getTxn().rollback();
            }
        }
    }

    public static void truncateFileMoves(Long userId) {
        Date oldestPermitted = DateTime.now().minusDays(RETENTION_DAYS).toDate();
        Objectify ofy = Datastore.beginTxn();
        try {
            Iterable<Key<FileMove>> fileMoveKeys = Datastore.query(FileMove.class)
                .ancestor(Datastore.key(User.class, userId))
                .filter("when <", oldestPermitted)
                .fetchKeys();
            
            Datastore.delete(fileMoveKeys);
            Datastore.commit();
        } finally {
            if (ofy.getTxn().isActive()) {
                ofy.getTxn().rollback();
            }
        }
    }
}
