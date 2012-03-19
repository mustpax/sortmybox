package models;

import java.util.Date;

import org.joda.time.DateTime;

import siena.Id;
import siena.Model;
import siena.Query;

public class FileMove extends Model {
    public static final int RETENTION_DAYS = 3;

    @Id
    public Long id;

    public String fromFile;

    public String toDir;

    public Date when;
    
    public Long owner;
    
    public Boolean successful;
    
    public FileMove(Rule rule, String from, boolean success) {
        this.owner = rule.owner;
        this.toDir = rule.dest;
        this.fromFile = from;
        this.when = new Date();
        this.successful = success;
    }
    
    public static Query<FileMove> all() {
        return Model.all(FileMove.class);
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

    public static int deleteStaleForUser(Long user) {
        Date oldestPermitted = DateTime.now().minusDays(RETENTION_DAYS).toDate();
        return all().filter("owner", user)
				    .filter("when<", oldestPermitted).delete();
    }

    public static FileMove findById(Long id) {
        return all().filter("id", id).get();
    }
}
