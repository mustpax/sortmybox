package rules;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import models.FileMove;
import models.Rule;
import models.User;
import play.Logger;

import com.google.appengine.repackaged.com.google.common.base.Pair;
import com.google.common.collect.Lists;

import dropbox.Dropbox;
import dropbox.client.DropboxClient;
import dropbox.client.DropboxClientFactory;
import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;

public class RuleUtils {
    private static final String INVALID_CHAR_REPLACEMENT = "-";
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
    
    public static String getExt(String fileName) {
        return splitName(fileName).second;
    }

    /**
     * Split given fileName into name and extension.
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
     * @return a {@link Pair} where first is file name and second is extension.
     * Extension may be null
     * Extension does not contain the leading period (.)
     */
    public static Pair<String, String> splitName(String fileName) {
        if (fileName == null) {
            return new Pair<String, String>(null, null);
        }

        int extBegin = fileName.lastIndexOf(".");

        if (extBegin <= 0) {
	        return new Pair<String, String>(fileName, null);
        }

        String name = fileName.substring(0, extBegin);
        String ext = fileName.substring(extBegin + 1);
        if (ext.isEmpty()) {
            ext = null;
        }

        return new Pair<String, String>(name, ext);
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
        try {
            //Rebranding from Sortbox to SortMyBox requires backwards compatibility
            Set<String> files = client.listDir(user.sortingFolder);	

            if (files.isEmpty()) {
                Logger.info("Ran rules for %s, no files to process.", user);
                return fileMoves;
            }

            List<Rule> rules = Rule.findByUserId(user.id);
            Logger.info("Running rules for %s", user);

            for (String file : files) {
                String base = basename(file);
                for (Rule r : rules) {
                    if (r.matches(base)) {
                        Logger.info("Moving file '%s' to '%s'. Rule id: %s",
                                file, r.dest, r.id);
                        boolean success = true;
                        String resolvedName = null;
                        for (int tries = 0; tries < MAX_TRIES; tries++) {
                            try {
                                String suffix = null;
                                if (!success) {
                                    suffix = " conflict"
                                            + (tries > 1 ? " " + tries : "");
                                }

                                resolvedName = removeInvalidChars(insertIntoName(base, suffix));

                                String dest = r.dest +
                                              (r.dest.endsWith("/") ? "" : "/") +
                                              resolvedName;
                                client.move(file, dest);
                                break;
                            } catch (FileMoveCollisionException e) {
                                success = false;
                                resolvedName = null;
                            }
                        }

                        if (!success && (resolvedName == null)) {
                            Logger.error("Cannot move file '%s' to '%s' after %d tries. Skipping.",
	                                     file, r.dest, MAX_TRIES);
                        }

                        fileMoves.add(new FileMove(user.id, base, r.dest, success, resolvedName));
                        break;
                    }
                }
            }

            Logger.info("Done running rules for %s. %d moves performed", user,
                    fileMoves.size());
            if (!fileMoves.isEmpty()) {
                user.incrementFileMoves(fileMoves.size());
                FileMove.save(fileMoves);
            }

            return fileMoves;
        } catch (InvalidTokenException e) {
            Logger.error(e, "Disabling periodic sort, invalid OAuth token for user: %s", user);
            user.periodicSort = false;
            user.save();
        }
        return Collections.emptyList();
    }
    
    /**
     * @return file name with invalid file name characters replaced with
     * {@link #INVALID_CHAR_REPLACEMENT}
     */
    private static String removeInvalidChars(String name) {
        if (Dropbox.isValidFilename(name)) {
            return name;
        }

        return Dropbox.DISALLOWED_FILENAME_CHARS
                      .matcher(name)
                      .replaceAll(INVALID_CHAR_REPLACEMENT);
    }

    public static String insertIntoName(String fileName, String suffix) {
        assert ! fileName.contains("/") : "Cannot process paths, can only process basenames.";
        Pair<String, String> fileAndExt = splitName(fileName);
        return fileAndExt.first +
               (suffix == null ? "" : suffix) +
               (fileAndExt.second == null ? "" : "." + fileAndExt.second);
    }

    public static String basename(String path) {
        return path == null ? null : new File(path).getName();
    }

    private RuleUtils() {}

}
