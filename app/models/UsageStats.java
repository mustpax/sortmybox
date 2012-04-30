package models;

import java.util.Date;

import javax.persistence.Id;

import play.modules.objectify.Datastore;

import com.google.common.base.Objects;

public class UsageStats {

    @Id public Long id;
    public Integer users;
    public Integer rules;
    public Integer fileMoves;
    public Date created;

    public UsageStats(Integer users, Integer rules, Integer fileMoves) {
        this.users = users;
        this.rules = rules;
        this.fileMoves = fileMoves;
        this.created = new Date();
    }
    
    public void save() {
        Datastore.put(this);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(User.class)
            .add("id", id)
            .add("users", users)
            .add("rules", rules)
            .add("fileMoves", fileMoves)
            .toString();
    }

}
