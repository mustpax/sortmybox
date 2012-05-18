package models;

import com.google.appengine.api.datastore.Query;
import com.google.common.base.Supplier;

/**
 * Supplier for Query object
 * 
 * @author syyang
 */
public class QuerySupplier implements Supplier<Query> {
    
    private final String kind;
    
    public QuerySupplier(String kind) {
        this.kind = kind;
    }
    
    @Override
    public Query get() {
        return new Query(kind);
    }
}
