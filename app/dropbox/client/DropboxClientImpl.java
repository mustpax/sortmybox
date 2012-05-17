package dropbox.client;

import java.util.Map;
import java.util.Set;

import oauth.signpost.OAuth;
import play.Logger;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import dropbox.DropboxOAuthServiceInfoFactory;
import dropbox.DropboxURLs;
import dropbox.gson.DbxAccount;
import dropbox.gson.DbxMetadata;

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
    public DbxMetadata getMetadata(String path) {
        Preconditions.checkNotNull(path, "Path missing.");
        path = path.startsWith("/") ? path : "/" + path;

        WSRequest ws = new WSRequestFactory(DropboxURLs.METADATA, token, secret)
            .addPath(path)
            .addPair("include_deleted", "false")
            .create();

        try {
            HttpResponse resp = ws.get();
            if (resp.success()) {
                DbxMetadata ret = new Gson().fromJson(resp.getJson(), DbxMetadata.class);
                if (ret.isDeleted()) {
                    return null;
                }
                return ret;
            }
            Logger.warn("Failed getting metadata for '%s'. %s", path, getError(resp));
        } catch (RuntimeException e) {
            Logger.warn(e, "Exception while trying to fetch metadata for '%s'.");
        }
        return null;
    }

    private static String sanitizeStatus(Integer status) {
        return status == null ? "null" : status.toString();
    }

    @Override
    public Set<String> listDir(String path) {
        return listDir(path, ListingType.FILES);
    }

    @Override
    public Set<String> listDir(String path, ListingType listingType) {
        Set<String> files = Sets.newHashSet();
        DbxMetadata metadata = getMetadata(path);

        if (metadata != null) {
            if (!metadata.isDir) {
                throw new IllegalArgumentException("Expecting dir, got a file: " + path);
            }

            for (DbxMetadata entry: metadata.contents) {
                if (entry.isDir && listingType.includeDirs) {
                    files.add(entry.path);
                }

                if ((!entry.isDir) && listingType.includeFiles) {
                    files.add(entry.path);
                }
            }
        }
        return files;
    }
    
    @Override
    public DbxMetadata mkdir(String path) {
        Preconditions.checkNotNull(path, "Path missing.");
        Preconditions.checkArgument(path.charAt(0) == '/', "Path should start with /.");

        WSRequest ws = new WSRequestFactory(DropboxURLs.CREATE_FOLDER, token, secret)
            .addPair("root", "dropbox")
            .addPair("path", path)
            .create();

        HttpResponse resp = ws.get();
        if (resp.success()) {
            return new Gson().fromJson(resp.getJson(), DbxMetadata.class);
        }

        Logger.error("Failed creating folder at '%s'. %s", path, getError(resp));
        return null;
    }
    
    @Override
    public DbxMetadata move(String from, String to) throws FileMoveCollisionException {
        Preconditions.checkArgument(from != null && to != null,
                "To and from paths cannot be null.");
        Preconditions.checkArgument((from.charAt(0) == '/') && (to.charAt(0) == '/'),
                "To and from paths should start with /");
        
        WSRequest ws = new WSRequestFactory(DropboxURLs.MOVE, token, secret)
            .addPair("root", "dropbox")
            .addPair("from_path", from)
            .addPair("to_path", to)
            .create();
        HttpResponse resp = ws.post();
        
        if (resp.success()) {
            Logger.info("Successfully moved files. From: '%s' To: '%s'", from, to);
            return new Gson().fromJson(resp.getJson(), DbxMetadata.class);
        }

        String err = getError(resp);
        Logger.warn("Failed to move files. " + err);
        if (Integer.valueOf(403).equals(resp.getStatus())) {
            throw new FileMoveCollisionException(err);
        }

        return null;
    }
    
    private static String getError(HttpResponse resp) {
        String error;
        try {
	        error = resp.getJson().getAsJsonObject().get("error").getAsString();
        } catch (UnsupportedOperationException e) {
            Logger.error(e, "Cannot parse error response from Dropbox. Resp: %s", resp.getStatus());
            error = "[cannot read error message]";
        }
        return String.format("Status: %s Message: %s", sanitizeStatus(resp.getStatus()), error);
    }
    
    /**
     * Builder for constructing a {@link WSRequest}.
     */
    private static class WSRequestFactory {
        private final String url;
        private final String token;
        private final String secret;
        private final Map<String, String> pairs;
        private String path = null;
        
        public WSRequestFactory(DropboxURLs url, String token, String secret) {
            this.url = url.getPath();
            this.token = token;
            this.secret = secret;
            this.pairs = Maps.newLinkedHashMap();
        }
        
        public WSRequestFactory addPath(String path) {
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
                fullUrl.append("/").append(encodeParam("dropbox")).append(encodePath(path));
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
            if ((param == null) || param.isEmpty()) {
                return "";
            }

            String[] nodes = param.split("/");
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = encodeParam(nodes[i]);
            }

            return Joiner.on("/").join(nodes);
        }

        private static String encodeParam(String param) {
            return OAuth.percentEncode(param);
        } 
    }

}
