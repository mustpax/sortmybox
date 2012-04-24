package models;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import rules.RuleType;

public class FileMove {

    public static final String KIND = FileMove.class.getSimpleName();
    public static final int RETENTION_DAYS = 3;

    public static final Function<Entity, Key> TO_KEY = new Function<Entity, Key>() {
        @Override public Key apply(Entity entity) {
            return entity.getKey();
        }
    };

    private static final Function<FileMove, Entity> TO_ENTITY = new Function<FileMove, Entity>() {
        @Override
        public Entity apply(FileMove fileMove) {
            return fileMove.toEntity();
        }
    };
    
    public Long id;
    public String fromFile;
    public String toDir;
    public Long owner;
    public Date when;
    public Boolean successful;
    
    public FileMove(Long owner, Rule rule, String from, boolean success) {
        this.toDir = rule.dest;
        this.fromFile = from;
        this.when = new Date();
        this.owner = owner;
        this.successful = success;
    }
    
    public FileMove(Entity entity) {
        this.id = entity.getKey().getId();
        this.fromFile = (String) entity.getProperty("fromFile");
        this.toDir = (String) entity.getProperty("toDir");
        this.when = (Date) entity.getProperty("when");
        this.owner = (Long) entity.getProperty("owner");
        this.successful = (Boolean) entity.getProperty("successful");
    }

    public Entity toEntity() {
        Key parentKey = KeyFactory.createKey(User.KIND, owner);
        Entity entity = new Entity(KIND, parentKey);
        entity.setProperty("fromFile", fromFile);
        entity.setProperty("toDir", toDir);
        entity.setProperty("when", when);
        entity.setProperty("owner", owner);
        entity.setProperty("successful", isSuccessful());
        return entity;
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

    public static FileMove findById(User owner, long id) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();    
        try {
            Key key = KeyFactory.createKey(owner.getKey(), KIND, id);
            return new FileMove(ds.get(key));
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    public static Iterator<FileMove> findByOwner(User owner, int limit) {
        Preconditions.checkNotNull(owner, "owner cannot be null");
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Query q = new Query(KIND)
            .setAncestor(owner.getKey())
            .addFilter("when", FilterOperator.GREATER_THAN, owner.created)
            .addSort("when", SortDirection.DESCENDING);
        PreparedQuery pq = ds.prepare(q);
        pq.asIterator(FetchOptions.Builder.withLimit(limit));
        return Iterators.transform(pq.asIterator(), new Function<Entity, FileMove>() {
            @Override public FileMove apply(Entity entity) {
                return new FileMove(entity);
            }
        });
    }

    public static void insert(final User owner, List<FileMove> fileMoves) {
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Transaction tx = ds.beginTransaction();
        try {
            ds.put(tx, Lists.transform(fileMoves, TO_ENTITY));
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
    }

    public static void truncateFileMoves(User owner) {
        Date oldestPermitted = DateTime.now().minusDays(RETENTION_DAYS).toDate();
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();        
        Transaction tx = ds.beginTransaction();
        try {
            Query q = new Query(KIND)
                .setAncestor(owner.getKey())
                .addFilter("when", FilterOperator.LESS_THAN, oldestPermitted);
            PreparedQuery pq = ds.prepare(tx, q);
            ds.delete(tx, Iterables.transform(pq.asIterable(), TO_KEY));
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
    }
}
