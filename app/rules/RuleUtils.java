package rules;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import models.FileMove;
import models.Rule;
import models.User;
import play.Logger;

import com.google.common.collect.Lists;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.FileMoveCollisionException;

public class RuleUtils {
    private static final int MAX_TRIES = 10;

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
        return Pattern.compile(out.toString(), Pattern.CASE_INSENSITIVE);
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
    
    /**
     * Process all rules for the current user and move files to new location
     * as approriate.
     * 
     * @return list of file moves performed
     */
    public static List<FileMove> runRules(User user) {
        user.updateLastSyncDate();

        List<FileMove> fileMoves = Lists.newArrayList();
        DropboxClient client = DropboxClientFactory.create(user);
        Set<String> files = client.listDir(Dropbox.getSortboxPath());

        if (files.isEmpty()) {
            Logger.info("Ran rules for %s, no files to process.", user);
            return fileMoves;
        }

        List<Rule> rules = Rule.findByUserId(user.id);
        Logger.info("Running rules for %s", user);
        
        for (String file: files) {
            String base = basename(file);
            for (Rule r : rules) {
                if (r.matches(base)) {
                    Logger.info("Moving file '%s' to '%s'. Rule id: %s", file, r.dest, r.id);
                    boolean success = true;
                    String dest = null;
                    String suffix = null;
                    int tries = 0;
                    while (tries < MAX_TRIES) {
	                    try {
	                        dest = r.dest + (r.dest.endsWith("/") ? "" : "/") + base;
	                        if (suffix != null) {
	                            dest += suffix + (tries > 1 ? " " + tries : "");
	                        }
	                        client.move(file, dest);
	                        break;
	                    } catch (FileMoveCollisionException e) {
	                        success = false;
	                        suffix = "conflict";
	                    }
	                    tries++;
                    }

                    if (tries >= MAX_TRIES) {
                        Logger.error("Cannot move file '%s' to '%s' after %d tries. Skipping.", file, r.dest, MAX_TRIES);
                    }
                        
                    fileMoves.add(new FileMove(user.id, base, r.dest, success));
                    break;
                }
            }
        }
        
        Logger.info("Done running rules for %s. %d moves performed", user, fileMoves.size());
        if (!fileMoves.isEmpty()) {
            user.incrementFileMoves(fileMoves.size());
            FileMove.save(fileMoves);
        }

        return fileMoves;
    }
    
    private static String basename(String path) {
        return path == null ? null : new File(path).getName();
    }

    private RuleUtils() {}

}
