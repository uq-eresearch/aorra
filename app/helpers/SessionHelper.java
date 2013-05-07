package helpers;

import models.CacheableUser;
import play.Play;
import play.mvc.Http;
import providers.CacheableUserProvider;
import service.GuiceInjectionPlugin;

public class SessionHelper {

  public static CacheableUser currentUser() {
    return getProvider().getUser(Http.Context.current().session());
  }

  protected static CacheableUserProvider getProvider() {
    return Play.application().plugin(GuiceInjectionPlugin.class).getInjector()
        .getInstance(CacheableUserProvider.class);
  }

}
