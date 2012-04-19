package controllers;

import models.User;
import play.mvc.Controller;

public class Footer extends Controller {

    public static void team() {
        renderWithUser();
    }

    public static void terms() {
        renderWithUser();
    }
    
    public static void privacy() {
        renderWithUser();
    }
    
    public static void contact() {
        renderWithUser();
    }

    private static void renderWithUser() {
        User user = Login.getLoggedInUser();        
        if (user == null) {
            render();
        } else {
            render(user);
        }
    }
}
