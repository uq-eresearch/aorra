package helpers;

import com.google.inject.Injector;

import models.CacheableUser;
import play.Application;
import play.Play;
import play.mvc.Http;
import providers.CacheableUserProvider;
import service.GuiceInjectionPlugin;

public class SessionHelper {

  public static CacheableUser currentUser() {
    return appInstance()
        .getProvider()
        .getUser(Http.Context.current().session());
  }

  protected static SessionHelper appInstance() {
    return new SessionHelper(Play.application());
  }

  /* Protected instance */

  final Application application;

  protected SessionHelper(Application application) {
    this.application = application;
  }

  protected Injector getInjector() {
    return application.plugin(GuiceInjectionPlugin.class).getInjector();
  }

  protected CacheableUserProvider getProvider() {
    return getInjector().getInstance(CacheableUserProvider.class);
  }

}
