package controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.security.user.Group;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.jcrom.Jcrom;

import models.GroupManager;
import models.User;
import models.UserDAO;
import models.User.Invite;
import models.User.Login;

import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.google.inject.Inject;
import com.google.inject.Injector;

import static play.data.Form.form;
import play.Play;
import play.Logger;
import play.data.Form;
import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.JsonBuilder;

public final class Application extends SessionAwareController {

  @Inject
  Application(
      final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler) {
    super(sessionFactory, jcrom, sessionHandler);
  }

  @With(UncacheableAction.class)
  public final Result index() {
    if (!isAuthenticated()) return login();
    return getInjector().getInstance(FileStoreController.class).index();
  }

  @SubjectPresent
  public final Result invite() {
    return ok(views.html.Application.invite.render(form(Invite.class)));
  }

  @SubjectPresent
  public final Result postInvite() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<Invite> filledForm = form(Invite.class).bindFromRequest();
    if (filledForm.hasErrors()) {
      // User did not fill everything properly
      return badRequest(views.html.Application.invite.render(filledForm));
    } else {
      Result retval = UsernamePasswordAuthProvider.handleSignup(ctx());
      assiginCurrentUserGroups(filledForm.get().getEmail());
      return retval;
    }
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

  @SubjectPresent
  public final Result userExists(String encodedEmail) {
    final String email = urlDecode(encodedEmail);
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) throws Throwable {
        final UserDAO dao = getUserDAO(session);
        final User user = dao.findByEmail(email);
        return ok(views.html.Application.userExists.render(user));
      }
    });
  }

  public final Result userUnverified(String encodedEmail) {
    final String email = urlDecode(encodedEmail);
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) throws Throwable {
        final UserDAO dao = getUserDAO(session);
        final User user = dao.findByEmail(email);
        return ok(views.html.Application.userUnverified.render(user));
      }
    });
  }

  protected User assiginCurrentUserGroups(final String email) {
    final AuthUser authUser = PlayAuthenticate.getUser(ctx().session());


    return sessionFactory.inSession(new F.Function<Session, User>() {
      @Override
      public User apply(final Session session) throws RepositoryException {
        final GroupManager groupManager = new GroupManager(session);
        final UserDAO dao = getUserDAO(session);
        final User currentUser = dao.findByEmail(
            ((EmailIdentity) authUser).getEmail());
        final User newUser = dao.findByEmail(email);
        final Set<Group> gm =
            groupManager.memberships(currentUser.getJackrabbitUserId());
        for (Group group : gm) {
          final String groupName = group.getPrincipal().getName();
          try {
            groupManager.addMember(groupName, newUser.getJackrabbitUserId());
            Logger.info(
                "Added new user ("+newUser+") to group ("+groupName+").");
          } catch (PathNotFoundException e) {
            Logger.warn(
                "Unable to assign group ("+groupName+"): "+e.getMessage());
          }
        }
        return newUser;
      }
    });
  }

  protected UserDAO getUserDAO(Session session) {
    return new UserDAO(session, jcrom);
  }

  private boolean isAuthenticated() {
    return PlayAuthenticate.getUser(ctx().session()) != null;
  }

  private String urlDecode(String str) {
    try {
      return URLDecoder.decode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never happen
      throw new RuntimeException(e);
    }
  }

  private Injector getInjector() {
    return GuiceInjectionPlugin.getInjector(Play.application());
  }

}