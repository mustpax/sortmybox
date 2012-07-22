package box;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import rules.RuleUtils;
import box.Box.URLs;
import box.gson.BoxError;
import box.gson.BoxItem;
import box.gson.BoxName;
import box.gson.BoxMoveReq;

import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapMaker;
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
            switch (item.type) {
            case file:
                return this.type.includeFiles;
            case folder:
                return this.type.includeDirs;
            }

            throw new IllegalStateException("Unhandled BoxItem type: " + item.type);
        }
    }

    public final String token;

    private final Map<String, NullableItem> pathToItemMap = new MapMaker()
        .makeComputingMap(new Function<String, NullableItem>() {
            @Override
            public NullableItem apply(String path) {
                if (path == null || "/".equals(path)) {
                    return new NullableItem(getMetadata("0", BoxItem.Type.folder));
                }

                BoxItem parent = getItem(RuleUtils.getParent(path));
                if (parent == null) {
                    return NULL_ITEM;
                }

                // Child metadata is loaded as a mini item, we do a new fetch to get the full listing
                BoxItem miniChild = getChild(parent, RuleUtils.basename(path));
                if (miniChild == null || miniChild.id == null) {
                    return NULL_ITEM;
                }

                return new NullableItem(getMetadata(miniChild.id, miniChild.type));
            }
        });

    private static final NullableItem NULL_ITEM = new NullableItem(null);

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

    private static class NullableItem {
        private final BoxItem item;
        
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
        String toId = getId(RuleUtils.getParent(to));
        if (fromId == null ||
            toId   == null) {
            Logger.error("Failed to move file from %s to %s. Cannot resolve from file id.",
                         from, to);
            return;
        }

        Logger.info("Attempting to move file from: %s(%s) To: %s(%s)", from, fromId, RuleUtils.getParent(to), toId);
        HttpResponse resp = req("/files/" + fromId)
                .body(new Gson().toJson(new BoxMoveReq(toId, RuleUtils.basename(to))))
                .put();

        if (resp.success()) {
            BoxItem file = new Gson().fromJson(resp.getJson(), BoxItem.class);
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
    public boolean mkdir(String path) {
        Preconditions.checkNotNull(path, "Missing path.");
        String parentId = getId(RuleUtils.getParent(path));
        if (parentId ==  null) {
            Logger.error("Cannot create folder because parent directory doesn't exist: %s", path);
            return false;
        }

        HttpResponse resp = req("/folders/" + parentId)
                .body(new Gson().toJson(new BoxName(RuleUtils.basename(path))))
                .post();

        if (resp.success()) {
            BoxItem folder = new Gson().fromJson(resp.getJson(), BoxItem.class);
            Logger.info("Successfully created folder at path %s Folder: %s", path, folder);
            return true;
        }

        Logger.error("Failed creating directory '%s' Error: %s", path, getError(resp));
        return false;
    }

    @Override
    public boolean exists(String path) {
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
    public HttpResponse debug(HTTPMethod method, String url) throws InvalidTokenException{
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

    private @CheckForNull BoxItem getMetadata(@Nonnull String id, BoxItem.Type type) {
        if (id == null) {
            throw new NullPointerException("Parent id cannot be null");
        }

        String url = null;
        switch (type) {
        case file:
            url = "/files/";
            break;
        case folder:
            url = "/folders/";
            break;
        }

        Logger.info("getMetadata: id: %s type: %s", id, type);
        HttpResponse resp = req(url + id).get();
        if (resp.success()) {
            return new Gson().fromJson(resp.getJson(), BoxItem.class);
        }

        Logger.error("Box: Failed fetching folder info. Id: %s Error: %s",
                     id, getError(resp));
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

    private BoxItem getItem(String path) {
        return pathToItemMap.get(RuleUtils.normalize(path)).item;
    }

    private String getId(String path) {
        BoxItem item = getItem(path);
        return item == null ? null : item.id;
    }

    private WSRequest req(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        return WS.url(URLs.BASE_V2 + path)
                 .setHeader("Authorization",
                            String.format("BoxAuth api_key=%s&auth_token=%s", Box.API_KEY, this.token));
    }
}
