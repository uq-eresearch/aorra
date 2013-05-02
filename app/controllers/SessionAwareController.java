package controllers;

import javax.jcr.Session;

import org.jcrom.Jcrom;

import models.UserDAO;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;

import play.libs.F;
import play.mvc.Controller;
import service.JcrSessionFactory;

public abstract class SessionAwareController extends Controller {

  protected final JcrSessionFactory sessionFactory;
  protected final Jcrom jcrom;

  protected SessionAwareController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
  }

  protected <A extends Object> A inUserSession(final AuthUser authUser,
        final F.Function<Session, A> f) {
    String userId = sessionFactory.inSession(new F.Function<Session, String>() {
      @Override
      public String apply(Session session) {
        String email = authUser instanceof EmailIdentity ?
            ((EmailIdentity)authUser).getEmail() : authUser.getId();
        return (new UserDAO(session, jcrom))
            .findByEmail(email)
            .getJackrabbitUserId();
      }
    });
    return sessionFactory.inSession(userId, f);
  }

}
