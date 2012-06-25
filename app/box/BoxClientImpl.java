package box;

public class BoxClientImpl implements BoxClient {
    public final String token;

    BoxClientImpl(String token) {
        this.token = token;
    }
}
