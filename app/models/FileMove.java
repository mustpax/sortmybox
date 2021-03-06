package models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.MoreObjects;

public class FileMove implements Serializable {
    private static final FileMoveMapper MAPPER = new FileMoveMapper();

    private static final long serialVersionUID = 45L;

    public static final int RETENTION_DAYS = 90;

    public static final String KIND = "FileMove";
    
    public Long id;
    public String fromFile;
    public String toDir;
    public Date when;
    public Key owner;

    /**
     * True iff there was a filename collision when trying to move the file.
     */
    public Boolean hasCollision;
    
    /**
     * Files can be renamed while moving.
     * If this field is not null, it contains the actual name the file was saved under.
     * If this field is null, it means the file was saved under the name {@link #fromFile}
     */
    public String resolvedName;

    public FileMove() {}

    public FileMove(Key owner, String from, String toDir, boolean hasCollision, String resolvedName) {
        this.toDir = toDir;
        this.fromFile = from;
        this.when = new Date();
        this.hasCollision = hasCollision;
        this.owner = owner;
        this.resolvedName = resolvedName;
    }

    public FileMove(Key owner, String from, String dest, boolean hasCollision) {
	    this(owner, from, dest, hasCollision, null);
    }
    
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FileMove.class)
                      .add("id", id)
                      .add("fromFile", fromFile)
                      .add("toDir", toDir)
                      .add("when", when)
                      .add("owner", owner)
                      .add("hasCollision", hasCollision)
                      .toString();
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
            .append(this.hasCollision, other.hasCollision)
            .append(this.when, other.when)
            .append(this.owner, other.owner);
        return eq.isEquals();
    }

    public static List<FileMove> findByOwner(Key owner, int maxRows) {
        Query query = all().setAncestor(owner)
	                       .addSort("when", SortDirection.DESCENDING);
        return DatastoreUtil.asList(query,
			                        FetchOptions.Builder.withLimit(maxRows),
			                        MAPPER);
    }

    public static void save(List<FileMove> fileMoves) {
        DatastoreUtil.put(fileMoves, MAPPER);
    }

    public static Query all() {
        return new Query(KIND);
    }
    
    public static Key key(Key owner, long id) {
        return owner.getChild(KIND, id);
    }

    private static class FileMoveMapper implements Mapper<FileMove> {
        private FileMoveMapper() {}

        @Override
        public Entity toEntity(FileMove mv) {
            Entity entity = DatastoreUtil.newEntity(mv.owner, KIND, mv.id);
            entity.setUnindexedProperty("fromFile", mv.fromFile);
            entity.setUnindexedProperty("toDir", mv.toDir);
            entity.setUnindexedProperty("resolvedName", mv.resolvedName);
            entity.setProperty("when", mv.when);
            entity.setProperty("hasCollision", mv.hasCollision);
            return entity;
        }

        @Override
        public FileMove toModel(Entity entity) {
            FileMove mv = new FileMove();
            mv.id = entity.getKey().getId();
            mv.owner = entity.getKey().getParent();
            mv.fromFile = (String) entity.getProperty("fromFile");
            mv.toDir = (String) entity.getProperty("toDir");
            mv.resolvedName = (String) entity.getProperty("resolvedName");
            mv.when = (Date) entity.getProperty("when");
            mv.hasCollision = (Boolean) entity.getProperty("hasCollision");

            // If hasCollision column is null we read from the "successful" column
            // hasCollision is the inverse of success
            // If successful is null we assume success
            if (mv.hasCollision == null) {
	            Boolean success = (Boolean) entity.getProperty("successful");
	            if (success == null) {
	                mv.hasCollision = false;
	            } else {
	                mv.hasCollision = ! success;
	            }
            }
            return mv;
        }

        @Override
        public Class<FileMove> getType() {
            return FileMove.class;
        }

        @Override
        public Key toKey(FileMove model) {
            assert model.id != null : "Can't get key for FileMove that hasn't been persisted yet.";
            return key(model.owner, model.id);
        }
    }

}
