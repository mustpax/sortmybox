package models;

import play.Logger;
import play.modules.objectify.Datastore;
import tasks.FileMoveDeleter;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

public class CascadingDelete {

    private CascadingDelete() {}
    
    /**
     * Performs cascading delete on all data related to the given user.
     * 
     * @param user the user to delete
     */
    public static void delete(User user) {
        Preconditions.checkNotNull(user);
        
        // 1. delete rules
        Iterable<Key<Rule>> ruleKeys = Datastore.query(Rule.class)
            .ancestor(Datastore.key(User.class, user.id))
            .fetchKeys();
        Datastore.delete(ruleKeys);
        Logger.info("Deleted rules for user: %s", user);

        // 2. delete user
        user.delete();
        Logger.info("Deleted user: %s", user);

        // 3. enqueue a delete task to delete file moves
        FileMoveDeleter.submit(user);
    }
}
