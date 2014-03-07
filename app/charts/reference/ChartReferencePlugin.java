package charts.reference;

import play.Application;
import play.Plugin;
import service.GuiceInjectionPlugin;

import com.google.inject.Injector;

public class ChartReferencePlugin extends Plugin {

  private final Application application;

  public ChartReferencePlugin(Application application) {
    this.application = application;
  }

  @Override
  public void onStart() {
    injector().getInstance((ChartRefCache.class)).start();
  }

  @Override
  public void onStop() {
    injector().getInstance((ChartRefCache.class)).stop();
  }

  private Injector injector() {
    return GuiceInjectionPlugin.getInjector(application);
  }
}
