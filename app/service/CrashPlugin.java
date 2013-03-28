package service;

import java.util.HashMap;
import java.util.Map;

import play.Application;
import play.Logger;
import play.api.Plugin;

import org.crsh.plugin.PluginContext;
import org.crsh.plugin.PluginDiscovery;
import org.crsh.plugin.PluginLifeCycle;
import org.crsh.plugin.ServiceLoaderDiscovery;
import org.crsh.vfs.FS;
import org.crsh.vfs.Path;

public class CrashPlugin extends PluginLifeCycle implements Plugin {

  private final Application application;

  public CrashPlugin(Application application) {
    super();
    this.application = application;
  }

  @Override
  public boolean enabled() {
    return !("disabled".equals(
        application.configuration().getString("crashplugin", "enabled")));
  }

  @Override
  public void onStart() {
    PluginDiscovery discovery = new ServiceLoaderDiscovery(
        application.classloader());
    Logger.info("Starting CRaSH...");
    PluginContext context = new PluginContext(discovery, getAttributes(),
        getFS(Path.get("/crash/commands/")), getFS(Path.get("/crash/")),
        application.classloader());
    start(context);
  }

  @Override
  public void onStop() {
    stop();
  }

  protected Map<String, Object> getAttributes() {
    return new HashMap<String, Object>();
  }

  protected FS getFS(Path path) {
    try {
      FS fs = new FS();
      fs.mount(application.classloader(), path);
      return fs;
    } catch (Exception e) {
      // We really shouldn't get any errors here, and if we do they're fatal
      throw new RuntimeException(e);
    }
  }

}