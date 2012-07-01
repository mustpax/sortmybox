package box.gson;

import java.io.Serializable;

public class BoxName implements Serializable {
    public String name;
    
    public BoxName(String name) { 
        this.name = name;
    }
}
