package providers;

import java.util.concurrent.Callable;

import javax.jcr.Session;

import models.CacheableUser;
import models.User;
import models.UserDAO;

import org.jcrom.Jcrom;

import play.Logger;
import play.cache.Cache;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;
import service.JcrSessionFactory;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DeadboltHandlerImpl extends AbstractDeadboltHandler implements CacheableUserProvider {

  protected final JcrSessionFactory sessionFactory;
  protected final Jcrom jcrom;

  @Inject
  public DeadboltHandlerImpl(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
  }

  @Override
  public Result beforeAuthCheck(Context context) {
    // Nothing should be required - we're using security annotations
    return null;
  }

  @Override
  public Subject getSubject(Context context) {
    return getUser(context.session());
  }

  @Override
  public Result onAuthFailure(Context context, String content) {
    // TODO: Implement more user-friendly view
    return forbidden(content);
  }

  @Override
  public DynamicResourceHandler getDynamicResourceHandler(Context context) {
    // None for now
    return null;
  }

  @Override
  public CacheableUser getUser(Http.Session session) {
    final AuthUser authUser = PlayAuthenticate.getUser(session);
    // Not logged in
    if (authUser == null) return null;
    // Get from cache or JCR
    try {
      return Cache.getOrElse(authUser.getId(), new Callable<CacheableUser>() {
        @Override
        public CacheableUser call() throws Exception {
          return getUserFromJcr(authUser);
        }
      }, 60);
    } catch (Exception e) {
      // Nothing recoverable for now.
      throw new RuntimeException(e);
    }
  }

  protected CacheableUser getUserFromJcr(final AuthUser authUser) {
    User user = sessionFactory.inSession(new F.Function<Session, User>() {
      @Override
      public User apply(Session session) {
        String email = authUser instanceof EmailIdentity ?
            ((EmailIdentity)authUser).getEmail() : authUser.getId();
        Logger.debug("Lookup up user with email: " + email);
        return (new UserDAO(session, jcrom))
            .findByEmail(email);
      }
    });
    return new CacheableUser(authUser, user);
  }

}
