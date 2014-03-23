package box;

import common.api.ApiClient;

public interface BoxClient extends ApiClient {
    public BoxAccount getAccount();
}
