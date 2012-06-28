package box;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import rules.RuleUtils;
import box.Box.URLs;
import box.gson.BoxItem;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapMaker;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

public class BoxClientImpl implements BoxClient {
    public final String token;
    private final Map<String, String> pathToIdMap = new MapMaker()
        .makeComputingMap(new Function<String, String>() {
            @Override
            public String apply(String path) {
                if ("/".equals(path)) {
                    return "0";
                }

                String parentId = BoxClientImpl.this.pathToIdMap.get(RuleUtils.getParent(path));
                return getIdOfChild(parentId, RuleUtils.basename(path));
            }
        });
    
    BoxClientImpl(String token) {
        this.token = token;
    }

    @Override
    public void move(String from, String to) throws FileMoveCollisionException, InvalidTokenException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Set<String> listDir(String path) throws InvalidTokenException, NotADirectoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> listDir(String path, ListingType listingType) throws InvalidTokenException, NotADirectoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean mkdir(String path) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean exists(String path) {
        return false;
    }

    private static class ItemNameEquals implements Predicate<BoxItem> {
        private final String name;

        public ItemNameEquals(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(BoxItem item) {
            return this.name.equals(item.name);
        }
    }

    public static String getIdOfChild(JsonElement json, String child) {
        Type t = new TypeToken<List<BoxItem>>(){}.getType();
        List<BoxItem> items = new Gson().fromJson(json.getAsJsonObject()
                                                      .getAsJsonObject("item_collection")
                                                      .get("entries"),
                                                  t);
        
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
    
    private String getIdOfChild(String parentId, String child) {
        HttpResponse resp = req("/folders/" + parentId).get();
        
        if (resp.success()) {
            Type t = new TypeToken<List<BoxItem>>(){}.getType();
            List<BoxItem> items = new Gson().fromJson(resp.getJson()
                                                          .getAsJsonObject()
                                                          .get("item_collection"),
                                                      t);
            
            Collection<BoxItem> matching = Collections2.filter(items, new ItemNameEquals(child));
            if (matching.isEmpty()) {
                Logger.warn("Box: could not find child in parent. Parent id: %s Child: %s", parentId, child);
                return null;
            }

            if (matching.size() == 1) {
                return matching.iterator().next().id;
            }

            Logger.error("Box: more than one child returned from API under same parent. Parent id: %s Child: %s Matches: %s",
                         parentId, child, matching);
            return null;
        }

        Logger.error("Box: Failed fetching folder info. Parent id: %s Child: %s Error: ",
                    parentId, child, resp.getString());
        return null;
    }

    private String getId(String path) {
        return pathToIdMap.get(RuleUtils.normalize(path));
    }

    private WSRequest req(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        return WS.url(URLs.BASE_V2 + path)
                 .setHeader("Authorization",
                            String.format("BoxAuth api_key=%s&auth_token=%s", Box.API_KEY, this.token));
    }
}
