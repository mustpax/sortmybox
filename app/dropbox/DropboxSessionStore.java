package dropbox;

import com.dropbox.core.DbxSessionStore;

import play.mvc.Scope;

public class DropboxSessionStore implements DbxSessionStore {
    private static final String DROPBOX_SESS_KEY = "dropbox_aauth2";
    private final Scope.Session session;
    
    public DropboxSessionStore(Scope.Session session) {
        this.session = session;
    }

    @Override
    public String get() {
        return session.get(DROPBOX_SESS_KEY);
    }

    @Override
    public void set(String value) {
        session.put(DROPBOX_SESS_KEY, value);
    }

    @Override
    public void clear() {
        session.remove(DROPBOX_SESS_KEY);
    }
}
