package models;

import play.Logger;
import tasks.FileMoveDeleter;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.common.base.Preconditions;

public class CascadingDelete {

    private CascadingDelete() {}
    
    /**
     * Performs cascading delete on all data related to the given user.
     * 
     * @param user the user to delete
     */
    public static void delete(User user) {
        Preconditions.checkNotNull(user);
        
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        // Double limit to ensure we get every single rule
        FetchOptions fo = FetchOptions.Builder.withLimit(Rule.MAX_RULES * 2);
        ds.delete(DatastoreUtil.queryKeys(Rule.byOwner(user.id), fo, Rule.MAPPER));
        Logger.info("Deleted rules for user: %s", user);

        // 2. delete user
        user.delete();
        Logger.info("Deleted user: %s", user);

        // 3. enqueue a delete task to delete file moves
        FileMoveDeleter.submit(user);
    }
}
