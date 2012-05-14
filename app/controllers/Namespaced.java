package controllers;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

import com.google.appengine.api.NamespaceManager;

/**
 * Update namespace for requests.
 * 
 * @author mustpax
 */
public class Namespaced extends Controller {
    @Before(priority=1)
    static void setNamespace() {
        if (NamespaceManager.get() == null) {
            String namespace = System.getenv("NAMESPACE");
            if (namespace != null && ! namespace.isEmpty()) {
                Logger.info("Updating namespace to %s", namespace);
                NamespaceManager.set(namespace);
            }
        }
    }
    
}
