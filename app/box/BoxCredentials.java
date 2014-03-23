package box;

import com.google.gson.annotations.SerializedName;

public class BoxCredentials {
    @SerializedName("access_token")
    public String token;
    
    @SerializedName("refresh_token")
    public String refeshToken;
    
    @SerializedName("expires_in")
    public Long expiresIn;
}
