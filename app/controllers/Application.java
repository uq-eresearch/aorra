package controllers;

import javax.jcr.Session;

import org.jcrom.Jcrom;

import models.User;
import models.UserDAO;
import models.User.Login;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.google.inject.Inject;

import static play.data.Form.form;
import play.Logger;
import play.data.Form;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.JcrSessionFactory;

public final class Application extends Controller {

  private final JcrSessionFactory sessionFactory;
  private final Jcrom jcrom;

  @Inject
  Application(JcrSessionFactory sessionFactory, Jcrom jcrom) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
  }

  public final Result index() {
    if (!isAuthenticated()) return login();
    return redirect(controllers.routes.FileStoreController.upload());
  }

  public final Result login() {
    return ok(views.html.Application.login.render(form(Login.class)));
  }

  public final Result postLogin() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<Login> filledForm = form(Login.class).bindFromRequest();
    if (filledForm.hasErrors()) {
      // User did not fill everything properly
      return badRequest(views.html.Application.login.render(filledForm));
    } else {
      // Everything was filled
      return UsernamePasswordAuthProvider.handleLogin(ctx());
    }
  }

  public final Result verify(final String email, final String token) {
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) {
        User user = getUserDAO(session).findByEmail(email);
        if (user != null && user.checkVerificationToken(token)) {
          return ok(views.html.Application.setPassword.render(
              routes.Application.postVerify(email, token),
              form(User.ChangePassword.class)));
        } else {
          return forbidden();
        }
      }
    });
  }

  public final Result postVerify(final String email, final String token) {
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) {
        final UserDAO dao = getUserDAO(session);
        final User user = dao.findByEmail(email);
        if (user != null && user.checkVerificationToken(token)) {
          Form<User.ChangePassword> filledForm =
              form(User.ChangePassword.class).bindFromRequest();
          Logger.debug(filledForm+"");
          if (filledForm.hasErrors()) {
            return ok(views.html.Application.setPassword.render(
                routes.Application.postVerify(email, token),
                filledForm));
          }
          String clearPassword = filledForm.field("password").value();
          dao.setPassword(user, clearPassword);
          return PlayAuthenticate.loginAndRedirect(ctx(),
              new JackrabbitEmailPasswordAuthProvider.LoginUser(
                  clearPassword, email));
        } else {
          return forbidden();
        }
      }
    });
  }

  public final Result oAuthDenied(String providerKey) {
    // TODO: Implement
    return ok();
  }

  public final Result userExists() {
    // TODO: Implement
    return ok();
  }

  public final Result userUnverified() {
    // TODO: Implement
    return ok();
  }

  protected UserDAO getUserDAO(Session session) {
    return new UserDAO(session, jcrom);
  }

  private boolean isAuthenticated() {
    return PlayAuthenticate.getUser(ctx().session()) != null;
  }

}