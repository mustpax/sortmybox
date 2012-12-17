package remote;

import java.io.IOException;

import play.Logger;
import play.modules.gae.PlayDevEnvironment;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.google.apphosting.api.ApiProxy;

public abstract class RemoteScript {
    public abstract void innerRun();

    public void run() {
        RemoteApiOptions options = new RemoteApiOptions()
            .server("11-9-prod.sortmybox.appspot.com", 443)
            .credentials(System.getenv("user"), System.getenv("password"))
            .remoteApiPath("/remote_api");
        RemoteApiInstaller installer = new RemoteApiInstaller();
//        ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        try {
            installer.install(options);
            innerRun();
        } catch (IOException e) {
            Logger.error(e, "Unable to install");
        } finally {
            try {
                installer.uninstall();
            } catch (Throwable e) {
//                Logger.error(e, "Unable to uninstall");
            }
        }
    }
}
