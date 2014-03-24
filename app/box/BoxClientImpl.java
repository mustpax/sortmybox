package box;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import oauth.signpost.OAuth;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import rules.RuleUtils;
import box.Box.URLs;
import box.gson.BoxError;
import box.gson.BoxItem;
import box.gson.BoxMoveReq;
import box.gson.BoxName;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

public class BoxClientImpl implements BoxClient {
    public static class FileOrFolderPredicate implements Predicate<BoxItem> {
        private final ListingType type;
        
        public FileOrFolderPredicate(ListingType type) {
            this.type = type;
        }

        @Override
        public boolean apply(BoxItem item) {
            if (item.isFile()) {
                return this.type.includeFiles;
            }

            if (item.isFolder()) {
                return this.type.includeDirs;
            }

            Logger.warn("FileFolderPredicate: Unknown box item type: %s", item);
            return false;
        }
    }

    public final String token;

    private Cache<String, NullableItem> itemCache = CacheBuilder
            .newBuilder()
            .build(CacheLoader.from(new Function<String, NullableItem>() {
                @Override
                public NullableItem apply(String path) {
                    try {
                        if (path == null || "/".equals(path)) {
                            return new NullableItem(getMetadata("0", BoxItem.FOLDER));
                        }

                        BoxItem parent = getItem(RuleUtils.getParent(path));
                        if (parent == null || parent.id == null) {
                            return NULL_ITEM;
                        }

                        // Child metadata is loaded as a mini item, we do a new fetch to get the full listing
                        BoxItem miniChild = getChild(parent, RuleUtils.basename(path));
                        if (miniChild == null || miniChild.id == null) {
                            return NULL_ITEM;
                        }

                        if (miniChild.isFile()) {
                            return new NullableItem(miniChild);
                        }
                        // If we're fetching a folder, need to fetch it again to get contents
                        return new NullableItem(getMetadata(miniChild.id, miniChild.type));
                    } catch (InvalidTokenException e) {
                        return INVALID_TOKEN_ITEM;
                    }
                }
            }));

    private static final NullableItem NULL_ITEM = new NullableItem(null);
    private static final NullableItem INVALID_TOKEN_ITEM = new NullableItem(null);

    private static class ItemNameExtractor implements Function<BoxItem, String> {
        private final String parent;
        
        public ItemNameExtractor(String parent) {
            this.parent = parent;
        }

        @Override
        public String apply(BoxItem item) {
            return RuleUtils.normalize(parent + "/" + item.name, false);
        }
    }

    public static class NullableItem {
        public final BoxItem item;
        
        public NullableItem(BoxItem item) {
            this.item = item;
        }
    }

    BoxClientImpl(String token) {
        this.token = token;
    }

    @Override
    public void move(String from, String to) throws FileMoveCollisionException, InvalidTokenException {
        Logger.info("Move from %s to %s", from, to);
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        String fromId = getId(from);
        if (fromId == null) {
            Logger.error("Failed to move file from %s to %s. Cannot resolve from file id.",
                         from, to);
            return;
        }

        String parent = RuleUtils.getParent(to);
        BoxItem toItem = getItem(parent);
        if (toItem == null) {
            Logger.warn("BoxClient.move: Parent folder missing: %s", parent);
            toItem = mkdirItem(parent);

            if (toItem == null) {
                Logger.error("Cannot move file, failed creating parent.");
                return;
            }
        }

        if (! toItem.isFolder()) {
            Logger.error("Cannot move, parent item is not a folder. Parent: %s Item: %s", parent, toItem);
            return;
        }

        Logger.info("Attempting to move file from: %s(%s) To: %s(%s)", from, fromId, RuleUtils.getParent(to), toItem);
        HttpResponse resp = req("/files/" + fromId)
                .body(new Gson().toJson(new BoxMoveReq(toItem.id, RuleUtils.basename(to))))
                .put();

        if (resp.success()) {
            BoxItem file = new Gson().fromJson(resp.getJson(), BoxItem.class);
            invalidate(RuleUtils.getParent(from));
            invalidate(from);
            invalidate(RuleUtils.getParent(to));
            invalidate(to);
            Logger.info("Successfully moved file from %s to %s. File: %s",
                        from, to, file);
            return;
        }

        Logger.error("Failed moving from %s to %s Error: %s", from, to, getError(resp));
        // 400 indicates file name collision
        if (Integer.valueOf(400).equals(resp.getStatus())) {
            throw new FileMoveCollisionException(String.format("From: %s To: %s", from, to));
        }
    }

    @Override
    public Set<String> listDir(String path) throws InvalidTokenException, NotADirectoryException {
        return listDir(path, ListingType.FILES);
    }

    @Override
    public Set<String> listDir(String path, ListingType listingType) throws InvalidTokenException, NotADirectoryException {
        Set<String> ret = Sets.newHashSet();
        BoxItem item = getItem(path);

        if (item != null && item.children != null && item.children.entries != null) {
            addAll(ret, transform(filter(item.children.entries,
                                         new FileOrFolderPredicate(listingType)),
                                  new ItemNameExtractor(path)));
            return ret;
        }

        Logger.warn("listDir: Cannot find well formed metadata entry for %s", path);
        return ret;
    }

    @Override
    public boolean mkdir(String path) throws InvalidTokenException {
        return mkdirItem(path) != null;
    }

    private BoxItem mkdirItem(String path) throws InvalidTokenException {
        Preconditions.checkNotNull(path, "Missing path.");
        Logger.info("BoxClient.mkdir: path %s", path);
        String parent = RuleUtils.getParent(path);
        BoxItem parentItem = getItem(parent);

        if (parentItem ==  null) {
            Logger.warn("Parent folder doesn't exist, creating. Path: %s Parent: %s", path, parent);
            parentItem = mkdirItem(parent);

            if (parentItem == null) {
                Logger.error("Could not create parent directory: %s", parent);
                return null;
            }
        }

        if (! parentItem.isFolder()) {
            Logger.error("Cannot create directory because parent is not a directory. Parent path: %s Item: %s", parent, parentItem);
            return null;
        }

        HttpResponse resp = req("/folders/" + parentItem.id)
                .body(new Gson().toJson(new BoxName(RuleUtils.basename(path))))
                .post();

        if (resp.success()) {
            BoxItem folder = new Gson().fromJson(resp.getJson(), BoxItem.class);
            invalidate(path);
            Logger.info("Successfully created folder at path %s Folder: %s", path, folder);
            return folder;
        }

        Logger.error("Failed creating directory '%s' Error: %s", path, getError(resp));
        return null;
    }

    @Override
    public boolean exists(String path) throws InvalidTokenException {
        return getItem(path) != null;
    }

    /**
     * Make signed API request return the resulting HttpResponse
     * @param method HTTP method for the request
     * @param url full request URL with associated parameters
     * @return HTTP response
     */
    @Override
    @Nonnull
    public HttpResponse debug(HTTPMethod method, String url) throws InvalidTokenException {
        Preconditions.checkArgument(url.startsWith("/"), "url must start with /");
        
        WSRequest req = req(url);
        switch (method) {
        case GET:
            return req.get();
        case POST:
            return req.post();
        default:
            throw new IllegalArgumentException("Unhandled HTTP method");
        }
    }

    private static class ItemNameEquals implements Predicate<BoxItem> {
        private final String name;

        public ItemNameEquals(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(BoxItem item) {
            return this.name.equalsIgnoreCase(item.name);
        }
    }

    public static @CheckForNull BoxItem getChild(BoxItem parent, String child) {
        if (parent == null || parent.children == null || parent.children.entries == null) {
            return null;
        }

        List<BoxItem> items = parent.children.entries;

        Collection<BoxItem> matching = Collections2.filter(items, new ItemNameEquals(child));
        if (matching.isEmpty()) {
            Logger.warn("Box: could not find child in parent. Child: %s Parent: %s", child, parent);
            return null;
        }

        if (matching.size() == 1) {
            return matching.iterator().next();
        }

        Logger.error("Box: more than one child returned from API under same parent. Child: %s Matches: %s Parent: %s",
                     child, matching, parent);
        return null;
    }

    @Override
    public BoxAccount getAccount() {
        WSRequest req = req("/users/me");
        
        HttpResponse resp = req.get();
        if (resp.success()) {
            return new Gson().fromJson(resp.getJson(), BoxAccount.class);
        }
        
        throw new IllegalStateException("Failed fetching box account info. Status: " + resp.getStatus() +
                                        " Error: " + resp.getString());
    }

    private @CheckForNull BoxItem getMetadata(@Nonnull String id, String type) throws InvalidTokenException {
        if (id == null) {
            throw new NullPointerException("Parent id cannot be null");
        }

        WSRequest request;
        if (BoxItem.FOLDER.equals(type)) {
            request = req("/folders/" + OAuth.percentEncode(id))
                          .setParameter("limit", 1000);
        } else {
            request = req("/files/" + OAuth.percentEncode(id));
        }

        Logger.info("getMetadata: id: %s type: %s", id, type);
        HttpResponse resp = request.get();
        if (resp.success()) {
            return new Gson().fromJson(resp.getJson(), BoxItem.class);
        }

        String err = getError(resp);
        if (Integer.valueOf(401).equals(resp.getStatus())) {
            Logger.error("Box: Failed to fetch metadata because auth token has been revoked. Id: %s Error: %s",
                         id, err);
            throw new InvalidTokenException("Box auth token has been revoked.");
        } 

        Logger.error("Box: Failed to fetch metadata. Id: %s Error: %s",
                     id, err);
        return null;
    }

    private static String getError(HttpResponse resp) {
        assert ! resp.success() : "Cannot get error for successful response.";
        try {
            return new Gson().fromJson(resp.getJson(), BoxError.class).toString();
        } catch (RuntimeException e) {
            return resp.toString();
        }
    }

    private void invalidate(String path) {
        itemCache.invalidate(RuleUtils.normalize(path));
    }

    private BoxItem getItem(String path) throws InvalidTokenException {
        try {
            NullableItem ni = itemCache.get(RuleUtils.normalize(path));
            if (ni == INVALID_TOKEN_ITEM) {
                throw new InvalidTokenException("Box auth token has been revoked.");
            }
            return ni.item;
        } catch (ExecutionException e) {
            Logger.error(e, "Cannot load %s", path);
            return null;
        }
    }

    private String getId(String path) throws InvalidTokenException {
        BoxItem item = getItem(path);
        return item == null ? null : item.id;
    }

    private WSRequest req(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        return WS.url(URLs.BASE_V2 + path)
                 .setHeader("Authorization",
                            String.format("Authorization: Bearer %s", WS.encode(this.token)));
    }
}
