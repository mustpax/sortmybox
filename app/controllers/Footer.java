package controllers;

import models.User;
import play.mvc.Controller;

public class Footer extends Controller {

    public static void team() {
        renderInner();
    }

    public static void terms() {
        renderInner();
    }
    
    public static void privacy() {
        renderInner();
    }
    
    public static void contact() {
        renderInner();
    }

    private static void renderInner() {
        User user = Login.getLoggedInUser();        
        if (user == null) {
            render();
        } else {
            render(user);
        }
    }
}
