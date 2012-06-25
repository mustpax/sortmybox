package box;

public class BoxClientImpl implements BoxClient {
    private final String token;
    private final String secret;

    BoxClientImpl(String token, String secret) {
        this.token = token;
        this.secret = secret;
    }
}
