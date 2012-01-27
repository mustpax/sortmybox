package models;

import java.util.regex.Pattern;

/**
 * 
 * @author mustpax
 */
public class RuleUtils {

    /**
     * Return a regex pattern that will match the given glob pattern.
     *
     * Only ? and * are supported.
     * TODO use a memoizer to cache compiled patterns.
     * TODO Collapse consecutive *'s.
     */
    public static Pattern getGlobPattern(String glob) {
        if (glob == null) {
            return Pattern.compile("");
        }

        StringBuilder out = new StringBuilder();
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
            case '*':
                out.append(".*");
                break;
            case '?':
                out.append(".");
                break;
            case '.':
                out.append("\\.");
                break;
            case '\\':
                out.append("\\\\");
                break;
            default:
                out.append(c);
            }
        }
        return Pattern.compile(out.toString());
    }
    
    /**
     * Extract the file extension from the file name.
     *
     * If the file name starts with a period but does not contain any other
     * periods we say that it doesn't have an extension.
     *
     * Otherwise all text after the last period in the filename is taken to be
     * the extension even if it contains spaces.
     * 
     * Examples:
     * ".bashrc" has no extension
     * ".foo.pdf" has the extension pdf
     * "file.ext ension" has extension "ext ension"
     *
     * @return file extension
     */
    public static String getExtFromName(String fileName) {
        if (fileName == null) {
            return null;
        }

        int extBegin = fileName.lastIndexOf(".");

        if (extBegin <= 0) {
            return null;
        }

        String ret = fileName.substring(extBegin + 1);
        if (ret.isEmpty()) {
            return null;
        }

        return ret;
    }
    
    private RuleUtils() {}

}
