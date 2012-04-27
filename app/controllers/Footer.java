package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.With;

@With(ErrorReporter.class)
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
    
    public static void faq() {
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
