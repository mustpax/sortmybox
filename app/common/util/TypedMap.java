package common.util;

import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * Utilities for extracting typed values from Map<String, String>
 * 
 * @author syyang
 */
public class TypedMap {

    private final Map<String, String> map;
    
    public TypedMap(Map<String, String> jobData) {
        this.map = Preconditions.checkNotNull(jobData);
    }
    
    public String get(Object key, String defaultValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return defaultValue;
    }
    
    public String get(Object key) {
        return map.get(key);
    }
    
    public int get(Object key, int defaultValue) {
        if (map.containsKey(key)) {
            return Integer.valueOf(map.get(key));
        }
        return defaultValue; 
    }

    public long get(Object key, long defaultValue) {
        if (map.containsKey(key)) {
            return Long.valueOf(map.get(key));
        }
        return defaultValue; 
    }
}
