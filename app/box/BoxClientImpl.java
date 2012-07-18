package box;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;

import java.lang.reflect.Type;
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
import box.gson.BoxParent;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

public class BoxClientImpl implements BoxClient {
    public class FileOrFolderPredicate implements Predicate<BoxItem> {
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
    private final Map<String, NullableId> pathToIdMap = new MapMaker()
        .makeComputingMap(new Function<String, NullableId>() {
            @Override
            public NullableId apply(String path) {
                if (path == null || "/".equals(path)) {
                    return ROOT_ID;
                }

                String parentId = BoxClientImpl.this.getId(RuleUtils.getParent(path));
                if (parentId == null) {
                    return NULL_ID;
                }

                return new NullableId(getIdOfChild(parentId, RuleUtils.basename(path)));
            }
        });
    
    private static final NullableId NULL_ID = new NullableId(null);
    private static final NullableId ROOT_ID = new NullableId("0");
    
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

    private static class NullableId {
        private final String id;
        
        public NullableId(String id) {
            this.id = id;
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
                .body(new Gson().toJson(new BoxParent(toId)))
                .put();

        if (resp.success()) {
            BoxItem file = new Gson().fromJson(resp.getJson(), BoxItem.class);
            Logger.info("Successfully moved file from %s to %s. File: %s",
                        from, to, file);
            return;
        }

        Logger.error("Failed moving from %s to %s Error: %s", from, to, getError(resp));
    }

    @Override
    public Set<String> listDir(String path) throws InvalidTokenException, NotADirectoryException {
        return listDir(path, ListingType.FILES);
    }

    @Override
    public Set<String> listDir(String path, ListingType listingType) throws InvalidTokenException, NotADirectoryException {
        Set<String> ret = Sets.newHashSet();
        // TODO use metadata available during the get-id API call for directory listing
        String id = getId(path);

        if (id != null) {
            HttpResponse resp = req("/folders/" + id).get();
            addAll(ret, transform(filter(getChildren(resp.getJson()),
                                         new FileOrFolderPredicate(listingType)),
                                  new ItemNameExtractor(path)));
            return ret;
        }

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
        return getId(path) != null;
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

    public static List<BoxItem> getChildren(JsonElement json) {
        Type t = new TypeToken<List<BoxItem>>(){}.getType();
        return new Gson().fromJson(json.getAsJsonObject()
                                       .getAsJsonObject("item_collection")
                                       .get("entries"),
                                   t);
    }

    public static String getIdOfChild(JsonElement json, String child) {
        List<BoxItem> items = getChildren(json);
       
        Collection<BoxItem> matching = Collections2.filter(items, new ItemNameEquals(child));
        if (matching.isEmpty()) {
            Logger.warn("Box: could not find child in parent. Child: %s Response: %s", child, json);
            return null;
        }

        if (matching.size() == 1) {
            return matching.iterator().next().id;
        }

        Logger.error("Box: more than one child returned from API under same parent. Child: %s Matches: %s Response %s",
                     child, matching, json);
        return null;
        
    }
    
    private @CheckForNull String getIdOfChild(@Nonnull String parentId, String child) {
        if (parentId == null) {
            throw new NullPointerException("Parent id cannot be null");
        }

        HttpResponse resp = req("/folders/" + parentId).get();
        if (resp.success()) {
            return getIdOfChild(resp.getJson(), child);
        }

        Logger.error("Box: Failed fetching folder info. Parent id: %s Child: %s Error: ",
                     parentId, child, getError(resp));
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

    private String getId(String path) {
        return pathToIdMap.get(RuleUtils.normalize(path)).id;
    }

    private WSRequest req(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        return WS.url(URLs.BASE_V2 + path)
                 .setHeader("Authorization",
                            String.format("BoxAuth api_key=%s&auth_token=%s", Box.API_KEY, this.token));
    }
}
