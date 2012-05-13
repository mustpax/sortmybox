package models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

public class FileMove implements Serializable {

    private static final long serialVersionUID = 45L;

    public static final int RETENTION_DAYS = 90;

    private static final String KIND = "FileMove";
    
    public Long id;
    public String fromFile;
    public String toDir;
    public Date when;
    public Long owner;
    public Boolean successful;
    
    public FileMove() {}

    public FileMove(Long owner, String from, String dest, boolean success) {
        this.toDir = dest;
        this.fromFile = from;
        this.when = new Date();
        this.successful = success;
        this.owner = owner;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        FileMove other = (FileMove) obj;
        EqualsBuilder eq = new EqualsBuilder()
            .append(this.fromFile, other.fromFile)
            .append(this.toDir, other.toDir)
            .append(this.successful, other.successful)
            .append(this.when, other.when)
            .append(this.owner, other.owner);
        return eq.isEquals();
    }

    public static FileMove findById(Long id) {
        Key key = KeyFactory.createKey(KIND, id);
        return DatastoreUtil.get(key, FileMoveMapper.INSTANCE);
    }

    public static List<FileMove> findByOwner(Long owner, int maxRows) {
        Query query = new Query(KIND)
	                      .setAncestor(User.key(owner))
	                      .addSort("when", SortDirection.DESCENDING);
        return DatastoreUtil.asList(query,
                       FetchOptions.Builder.withLimit(maxRows),
                       FileMoveMapper.INSTANCE);
    }

    public static void save(List<FileMove> fileMoves) {
        DatastoreUtil.put(fileMoves, FileMoveMapper.INSTANCE);
    }

    public static Query all() {
        return new Query(KIND);
    }
    
    public static Key key(long owner, long id) {
        return User.key(owner).getChild(KIND, id);
    }

    private static class FileMoveMapper implements Mapper<FileMove> {

        static final FileMoveMapper INSTANCE = new FileMoveMapper();

        private FileMoveMapper() {}

        @Override
        public Entity toEntity(FileMove mv) {
            Entity entity = new Entity(toKey(mv));
            entity.setProperty("fromFile", mv.fromFile);
            entity.setProperty("toDir", mv.toDir);
            entity.setProperty("when", mv.when);
            entity.setProperty("owner", mv.owner);
            entity.setProperty("successful", mv.successful);
            return entity;
        }

        @Override
        public FileMove toModel(Entity entity) {
            FileMove mv = new FileMove();
            mv.id = entity.getKey().getId();
            mv.owner = entity.getKey().getParent().getId();
            mv.fromFile = (String) entity.getProperty("fromFile");
            mv.toDir = (String) entity.getProperty("toDir");
            mv.when = (Date) entity.getProperty("when");
            mv.successful = (Boolean) entity.getProperty("successful");
            return mv;
        }

        @Override
        public Class<FileMove> getType() {
            return FileMove.class;
        }

        @Override
        public Key toKey(FileMove model) {
            return key(model.owner, model.id);
        }
    }

}
