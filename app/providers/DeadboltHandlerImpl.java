package providers;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.CacheableUser;
import models.User;
import models.UserDAO;

import org.jcrom.Jcrom;

import play.Logger;
import play.cache.Cache;
import play.libs.F;
import play.mvc.Http;
import service.JcrSessionFactory;
import service.filestore.roles.Admin;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DeadboltHandlerImpl implements CacheableUserProvider {

  protected final JcrSessionFactory sessionFactory;
  protected final Jcrom jcrom;

  @Inject
  public DeadboltHandlerImpl(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
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
    } catch (NullPointerException e) {
      // User may have been deleted
      return null;
    } catch (Exception e) {
      // Nothing recoverable for now.
      throw new RuntimeException(e);
    }
  }

  protected CacheableUser getUserFromJcr(final AuthUser authUser) {
    final List<String> roles = new LinkedList<String>();
    User user = sessionFactory.inSession(new F.Function<Session, User>() {
      @Override
      public User apply(Session session) throws RepositoryException {
        String email = authUser instanceof EmailIdentity ?
            ((EmailIdentity)authUser).getEmail() : authUser.getId();
        Logger.debug("Lookup up user with email: " + email);
        User user = (new UserDAO(session, jcrom))
            .findByEmail(email);
        if (isAdmin(session, user))
          roles.add("admin");
        return user;
      }

      private boolean isAdmin(final Session s, final User u)
          throws RepositoryException {
        return Admin.getInstance(s).getGroup()
            .isMember((new UserDAO(s, jcrom)).jackrabbitUser(u));
      }
    });
    return new CacheableUser(authUser.getProvider(), user, roles);
  }

}
