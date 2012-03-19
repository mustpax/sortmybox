package dropbox.client;

import java.util.List;
import java.util.Map;
import java.util.Set;

import oauth.signpost.OAuth;

import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import dropbox.Dropbox;
import dropbox.DropboxOAuthServiceInfoFactory;
import dropbox.DropboxURLs;
import dropbox.Dropbox.Root;
import dropbox.gson.DbxMetadata;
import dropbox.gson.DbxAccount;

/**
 * REST API Client for Dropbox
 * 
 * @author mustpax
 * @author syyang
 */
class DropboxClientImpl implements DropboxClient {
    
    private final String token;
    private final String secret;
    
    DropboxClientImpl(String token, String secret) {
        this.token = Preconditions.checkNotNull(token, "Token can't be null.");
        this.secret = Preconditions.checkNotNull(secret, "Secret can't be null");
    }

    @Override
    public DbxAccount getAccount() {
        WSRequest ws = new WSRequestFactory(DropboxURLs.ACCOUNT, token, secret).create();
        return new Gson().fromJson(ws.get().getJson(), DbxAccount.class);
    }

    @Override
    public Set<String> listDir(String path) {
        Preconditions.checkNotNull(path, "Path missing.");
        Preconditions.checkArgument(path.charAt(0) == '/', "Path should start with /.");
           
        WSRequest ws = new WSRequestFactory(DropboxURLs.METADATA, token, secret)
            .addPath(Dropbox.getRoot(), path)
            .create();
        HttpResponse resp = ws.get();
        
        Set<String> files = Sets.newHashSet();
        if (resp.success()) {
            DbxMetadata metadata = new Gson().fromJson(resp.getJson(), DbxMetadata.class);
            if (!metadata.isDir) {
                throw new IllegalArgumentException("Expecting dir, got a file: " + path);
            }

            for (DbxMetadata entry: metadata.contents) {
                if (!entry.isDir) {
                    files.add(entry.path);
                }
            }
        } else {
            Logger.error("Failed listing '%s'. %s", path, getError(resp));
        }
        return files;
    }
    
    @Override
    public DbxMetadata move(String from, String to) throws FileMoveCollisionException {
        Preconditions.checkArgument(from != null && to != null,
                "To and from paths cannot be null.");
        Preconditions.checkArgument((from.charAt(0) == '/') && (to.charAt(0) == '/'),
                "To and from paths should start with /");
        
        WSRequest ws = new WSRequestFactory(DropboxURLs.MOVE, token, secret)
            .addPair("root", Dropbox.getRoot().getPath())
            .addPair("from_path", from)
            .addPair("to_path", to)
            .create();
        HttpResponse resp = ws.post();
        
        if (resp.success()) {
            Logger.info("Successfully moved files. From: '%s' To: '%s'", from, to);
            return new Gson().fromJson(resp.getJson(), DbxMetadata.class);
        }

        String err = getError(resp);
        Logger.warn("Failed to move files. Error: %s", err);
        if (Integer.valueOf(403).equals(resp.getStatus())) {
            throw new FileMoveCollisionException(err);
        }

        return null;
    }
    
    private static String getError(HttpResponse resp) {
        return resp.getJson().getAsJsonObject().get("error").getAsString();
    }
    
    /**
     * Builder for constructing a {@link WSRequest}.
     */
    private static class WSRequestFactory {
        private final String url;
        private final String token;
        private final String secret;
        private final Map<String, String> pairs;
        private String root = null;
        private String path = null;
        
        public WSRequestFactory(DropboxURLs url, String token, String secret) {
            this.url = url.getPath();
            this.token = token;
            this.secret = secret;
            this.pairs = Maps.newLinkedHashMap();
        }
        
        public WSRequestFactory addPath(Root root, String path) {
            this.root = root.getPath();
            this.path = path;
            return this;
        }
        
        public WSRequestFactory addPair(String key, String value) {
            pairs.put(key, value);
            return this;
        }
        
        public WSRequest create() {
            StringBuilder fullUrl = new StringBuilder(url);
            if (path != null) {
                fullUrl.append("/").append(root).append(encodePath(path));
            }
            if (pairs.size() > 0) {
                fullUrl.append("?");
                boolean any = false;
                for (Map.Entry<String, String> entry : pairs.entrySet()) {
                    if (any) fullUrl.append("&");
                    String encoded = encodeParam(entry.getValue());
                    fullUrl.append(entry.getKey()).append("=").append(encoded);
                    any = true;
                }
            }
            ServiceInfo serviceInfo = DropboxOAuthServiceInfoFactory.create();
            return WS.url(fullUrl.toString()).oauth(serviceInfo, token, secret);
        }
        
        /**
         * WS.oauth() signing does not play nice with full URL encoded
         * file paths. So we have to use %20 instead of + and not 
         * escape "/" seperators
         */
        private static String encodePath(String param) {
            // XXX this will incorrectly unescape %%2F
            return encodeParam(param).replaceAll("%2F", "/");
        }

        private static String encodeParam(String param) {
            // XXX should we toLowerCase here?
            return OAuth.percentEncode(param.toLowerCase());
        } 
    }
}
