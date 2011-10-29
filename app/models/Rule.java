package models;

public class Rule {
    public RuleType type;
    public String pattern;
    public String dest;
    
    @Override
    public String toString() {
        return String.format("Rule Type: %s Pattern: '%s' Dest: '%s'",
                             this.type, this.pattern, this.dest);
    }
    
    public static enum RuleType {
        NAME_CONTAINS {
            @Override
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
            }
        },
        NAME_GLOB {
            @Override
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
            }
        },
        EXT_EQ {
            @Override 
            public boolean matches(String pattern, String fileName) {
                // TODO Auto-generated method stub
                return false;
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
    }
}
