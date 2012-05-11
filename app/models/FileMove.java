package models;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

/**
 * POJO for file moves.
 */
public class FileMove implements Serializable {

    private static final long serialVersionUID = 45L;

    public static final String KIND = FileMove.class.getSimpleName();
    public static final int RETENTION_DAYS = 7;
    
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

    public static Query all() {
        return new Query(KIND);
    }
    
    public static FileMove findById(Long id) {
        Key key = KeyFactory.createKey(KIND, id);
        return DatastoreUtil.get(key, FileMoveMapper.INSTANCE);
    }

    public static List<FileMove> findByOwner(Long owner, int maxRows) {
        Query query = new Query(FileMove.class.getSimpleName());
        query.addFilter("owner", FilterOperator.EQUAL, owner);
        return DatastoreUtil.asList(query,
                       FetchOptions.Builder.withLimit(maxRows),
                       FileMoveMapper.INSTANCE);
    }

    public static void save(List<FileMove> fileMoves) {
        DatastoreUtil.put(fileMoves, FileMoveMapper.INSTANCE);
    }


    private static class FileMoveMapper implements Mapper<FileMove> {

        static final FileMoveMapper INSTANCE = new FileMoveMapper();

        private FileMoveMapper() {}

    	@Override
		public Key getKey(FileMove mv) {
			return KeyFactory.createKey(KIND, mv.id);
		}
    	
        @Override
        public Entity toEntity(FileMove mv) {
            Entity entity = DatastoreUtil.newEntity(KIND, mv.id);
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
            mv.fromFile = (String) entity.getProperty("fromFile");
            mv.toDir = (String) entity.getProperty("toDir");
            mv.when = (Date) entity.getProperty("when");
            mv.owner = (Long) entity.getProperty("owner");
            mv.successful = (Boolean) entity.getProperty("successful");
            return mv;
        }
    }
}
