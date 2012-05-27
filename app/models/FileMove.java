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
import com.google.common.base.Objects;

public class FileMove implements Serializable {
    private static final FileMoveMapper MAPPER = new FileMoveMapper();

    private static final long serialVersionUID = 45L;

    public static final int RETENTION_DAYS = 90;

    public static final String KIND = "FileMove";
    
    public Long id;
    public String fromFile;
    public String toDir;
    public Date when;
    public Long owner;
    /**
     * True iff the file was moved to the destination folder with exactly the same name.
     * If the file was moved to the destination with a different name due to a file name
     * collision this flag will be false.
     */
    public Boolean successful;
    
    /**
     * Files can be renamed while moving.
     * If this field is not null, it contains the actual name the file was saved under.
     * If this field is null, it means the file was saved under the name {@link #fromFile}
     */
    public String resolvedName;

    public FileMove() {}

    public FileMove(Long owner, String from, String toDir, boolean success, String resolvedName) {
        this.toDir = toDir;
        this.fromFile = from;
        this.when = new Date();
        this.successful = success;
        this.owner = owner;
        this.resolvedName = resolvedName;
    }

    public FileMove(Long owner, String from, String dest, boolean success) {
	    this(owner, from, dest, success, null);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(FileMove.class)
                      .add("id", id)
                      .add("fromFile", fromFile)
                      .add("toDir", toDir)
                      .add("when", when)
                      .add("owner", owner)
                      .add("successful", successful)
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
            .append(this.successful, other.successful)
            .append(this.when, other.when)
            .append(this.owner, other.owner);
        return eq.isEquals();
    }

    public static List<FileMove> findByOwner(Long owner, int maxRows) {
        Query query = all().setAncestor(User.key(owner))
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
    
    public static Key key(long owner, long id) {
        return User.key(owner).getChild(KIND, id);
    }

    private static class FileMoveMapper implements Mapper<FileMove> {
        private FileMoveMapper() {}

        @Override
        public Entity toEntity(FileMove mv) {
            Entity entity = DatastoreUtil.newEntity(User.key(mv.owner), KIND, mv.id);
            entity.setUnindexedProperty("fromFile", mv.fromFile);
            entity.setUnindexedProperty("toDir", mv.toDir);
            entity.setUnindexedProperty("resolvedName", mv.resolvedName);
            entity.setProperty("when", mv.when);
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
            mv.resolvedName = (String) entity.getProperty("resolvedName");
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
            assert model.id != null : "Can't get key for FileMove that hasn't been persisted yet.";
            return key(model.owner, model.id);
        }
    }

}
