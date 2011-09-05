package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

/**
 * @author mustpax
 */
@With(RequiresLogin.class)
public class Application extends Controller {
    public static void index() {
        render();
    }
}