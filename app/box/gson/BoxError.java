package box.gson;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

public class BoxError implements Serializable {
    public String code;
    public Integer status;
    public String message;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                      .add("status", status)
                      .add("code", code)
                      .add("message", message)
                      .toString();
    }    
}
