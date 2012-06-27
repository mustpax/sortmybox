package box;

import java.util.Set;

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
}
