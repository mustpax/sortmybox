package box;

import com.google.gson.annotations.SerializedName;

public class BoxAccount {
    public long id;
    @SerializedName("login")
    public String email;
    public String name;

}
