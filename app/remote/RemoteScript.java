package remote;

import java.io.File;
import java.io.IOException;

import play.Logger;
import play.Play;
import play.modules.gae.PlayDevEnvironment;
import play.server.Server;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.google.apphosting.api.ApiProxy;

public abstract class RemoteScript {
    public abstract void innerRun();

    public void run() {
        RemoteApiOptions options = new RemoteApiOptions()
            .server("sortmybox-hrd.appspot.com", 443)
//            .credentials(System.getenv("user"), System.getenv("password"))
            .remoteApiPath("/remote_api")
            .useApplicationDefaultCredential();
        RemoteApiInstaller installer = new RemoteApiInstaller();
        File root = new File(System.getProperty("application.path"));
        Play.init(root, System.getProperty("play.id", ""));
        Play.start();
        try {
            installer.install(options);
            innerRun();
        } catch (IOException e) {
            Logger.error(e, "Unable to install");
        } finally {
            try {
                installer.uninstall();
            } catch (Throwable e) {
                Logger.error(e, "Unable to uninstall RemoteApi");
            }
        }
    }
}
