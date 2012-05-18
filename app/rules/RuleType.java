package rules;

import java.util.regex.Matcher;

public enum RuleType {
    
    NAME_CONTAINS {
        @Override
        public boolean matches(String pattern, String fileName) {
            if ((pattern  == null) ||
                (fileName == null)) {
                return false;
            }

            return fileName.toLowerCase().contains(pattern.toLowerCase());
        }
    },
    GLOB {
        @Override
        public boolean matches(String pattern, String fileName) {
            Matcher m = RuleUtils.getGlobPattern(pattern).matcher(fileName);
            return m.matches();
        }
    },
    EXT_EQ {
        @Override 
        public boolean matches(String pattern, String fileName) {
            String ext = RuleUtils.getExt(fileName);
            if ((ext == null) ||
                (pattern == null)) {
                return false;
            }

            return ext.equalsIgnoreCase(pattern);
        }
    };
    
    /**
     * Apply a match to a file based on the rule type.
     * 
     * @param pattern the pattern for the current rule
     * @param fileName file name to match against rule
     * @return true if given file name matches the current pattern
     */
    public abstract boolean matches(String pattern, String fileName);

    public static RuleType fromDbValue(String dbValue) {
        for (RuleType type : RuleType.values()) {
            if (type.name().equals(dbValue)) return type;
        }
        return null;
    }
}
