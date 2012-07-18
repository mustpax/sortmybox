package models;

import java.util.Date;
import java.util.HashSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

/**
 * Utils that allow us to calculate the active users uniqueFileMoveUsers
 * @author mojo
 *
 */
public class UserStatsUtil {
	
public static int countUniqueFileMoveUsers(String dateProperty, Date from, Date to, Query q) {
    	
    	Query query = q.addFilter(dateProperty, FilterOperator.GREATER_THAN_OR_EQUAL, from)
    					.addFilter(dateProperty, FilterOperator.LESS_THAN_OR_EQUAL, to)
    					.setKeysOnly();
    	
    	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
    	PreparedQuery pq = ds.prepare(query);
    	
    	// We use an iterator for loop to count all entities
    	HashSet hs = new HashSet();
        for (Entity e: pq.asIterable()) {
            hs.add(e.getParent());
        }
    	
    	return hs.size();
    }
    
}
