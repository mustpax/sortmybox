package controllers;

import models.User;
import play.mvc.Controller;
import play.mvc.With;

@With({ ErrorReporter.class, Namespaced.class })
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
    
    public static void faq() {
        renderInner();
    }
    
    public static void press() { 
    	renderInner();
    }
    
    private static void renderInner() {
        User user = Login.getUser();        
        if (user == null) {
            render();
        } else {
            render(user);
        }
    }
}
