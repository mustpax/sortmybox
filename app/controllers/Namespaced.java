package controllers;

import models.DatastoreUtil;
import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.modules.ModulesServiceFactory;

/**
 * Update namespace for requests.
 * 
 * @author mustpax
 */
public class Namespaced extends Controller {
    public static String getNamespace() {
        return System.getenv("NAMESPACE");
    }

    @Before
    static void addHeaders() {
        try {
            String namespace = getNamespace();
            if (namespace == null) {
                namespace = "";
            }
            response.setHeader("X-SMB-Namespace", namespace);
            response.setHeader("X-SMB-Module",
                               ModulesServiceFactory.getModulesService().getCurrentModule());
        } catch (Throwable t) {}
        
    }

    @Before(priority=1)
    static void setNamespace() {
        if (NamespaceManager.get() == null) {
            String namespace = getNamespace();
            if (namespace != null && ! namespace.isEmpty()) {
                Logger.info("Updating namespace to %s", namespace);
                NamespaceManager.set(namespace);
            }
        }

        if (! DatastoreUtil.isWritable()) {
            response.status = 503;
            render("Namespaced/maint.html");
        }
    }
}
