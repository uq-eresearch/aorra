import static play.Play.application;

import play.GlobalSettings;
import service.GuiceInjectionPlugin;
import com.google.inject.Injector;

public class Global extends GlobalSettings {

  @Override
  public <A> A getControllerInstance(Class<A> controllerClass)
      throws Exception {
    return getInjector().getInstance(controllerClass);
  }

  private Injector getInjector() {
    return GuiceInjectionPlugin.getInjector(application());
  }

}