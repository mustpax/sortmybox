package box;

import java.util.Set;

import box.Box.URLs;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

import dropbox.client.FileMoveCollisionException;
import dropbox.client.InvalidTokenException;
import dropbox.client.NotADirectoryException;

public class BoxClientImpl implements BoxClient {
    public final String token;

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
    
    private WSRequest req(String path) {
        path = path.startsWith("/") ? path : "/" + path;
        return WS.url(URLs.BASE_V2 + path)
                 .setHeader("Authorization",
                            String.format("BoxAuth api_key=%s&auth_token=%s", Box.API_KEY, this.token));
    }
}
