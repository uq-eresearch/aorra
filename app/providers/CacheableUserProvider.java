package providers;

import models.CacheableUser;
import play.mvc.Http;

public interface CacheableUserProvider {

  public abstract CacheableUser getUser(Http.Session session);

}