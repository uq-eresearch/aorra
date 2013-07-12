package controllers;

import static play.data.Form.form;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.CacheableUser;
import models.GroupManager;
import models.User;
import models.User.Invite;
import models.User.Login;
import models.UserDAO;

import org.apache.jackrabbit.api.security.user.Group;
import org.jcrom.Jcrom;

import play.Logger;
import play.Play;
import play.data.Form;
import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;

public final class Application extends SessionAwareController {

  @Inject
  Application(
      final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler) {
    super(sessionFactory, jcrom, sessionHandler);
  }

  @SubjectPresent
  public final Result invite() {
    return ok(views.html.Application.invite.render(form(Invite.class),
        availableGroups())).as("text/html; charset=utf-8");
  }

  @SubjectPresent
  public final Result postInvite() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<Invite> filledForm = form(Invite.class).bindFromRequest();
    if (filledForm.hasErrors()) {
      // User did not fill everything properly
      return badRequest(
          views.html.Application.invite.render(filledForm, availableGroups()));
    } else {
      Result retval = UsernamePasswordAuthProvider.handleSignup(ctx());
      final String[] groups =
          ctx().request().body().asFormUrlEncoded().get("groups[]");
      if (groups != null) {
        // Ensure only allowed groups are used from the selected list
        Set<String> assignableGroups =
            Sets.intersection(
                ImmutableSet.<String>copyOf(groups),
                availableGroups());
        assignUserGroups(filledForm.get().getEmail(), assignableGroups);
      }
      return retval;
    }
  }

  @With(UncacheableAction.class)
  public final Result login() {
    return ok(views.html.Application.login.render(form(Login.class)));
  }

  public final Result postLogin() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<Login> filledForm = form(Login.class).bindFromRequest();
    if (filledForm.hasErrors()) {
      Logger.debug(filledForm.errorsAsJson()+"");
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
      public Result apply(Session session) throws RepositoryException {
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

  @SubjectPresent
  public final Result changePassword() {
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) throws RepositoryException {
        final Map<String, String[]> params =
            ctx().request().body().asFormUrlEncoded();
        final UserDAO dao = getUserDAO(session);
        final User user = dao.get(getUser());
        if (!params.containsKey("newPassword") ||
            !params.containsKey("currentPassword")) {
          return badRequest("Current and new password required.")
              .as("text/plain");
        }
        final String currentPassword = params.get("currentPassword")[0];
        final String newPassword = params.get("newPassword")[0];
        if (dao.checkPassword(user, currentPassword)) {
          dao.setPassword(user, newPassword);
          return ok();
        } else {
          return badRequest("Incorrect current password.").as("text/plain");
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

  protected User assignUserGroups(final String email, final Set<String> groups){
    return sessionFactory.inSession(new F.Function<Session, User>() {
      @Override
      public User apply(final Session session) throws RepositoryException {
        final GroupManager groupManager = new GroupManager(session);
        final UserDAO dao = getUserDAO(session);
        final User newUser = dao.findByEmail(email);
        for (String groupName : groups) {
          final Group group = groupManager.find(groupName);
          if (group == null) {
            Logger.warn("Unable to resolve group ("+groupName+")");
            continue;
          }
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
    return PlayAuthenticate.getUser(ctx().session()) != null &&
        getUser() != null;
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

  private Set<String> availableGroups() {
    final CacheableUser user = getUser();
    return inUserSession(new F.Function<Session, Set<String>>() {

      @Override
      public Set<String> apply(Session session) throws Throwable {
        final ImmutableSet.Builder<String> l = ImmutableSet.builder();
        final GroupManager gm = new GroupManager(session);
        final Iterable<Group> availableGroups = user.hasRole("admin") ?
            gm.list() :
            gm.memberships(user.getJackrabbitUserId());
        for (final Group g : availableGroups) {
          l.add(g.getPrincipal().getName());
        }
        return l.build();
      }

    });
  }

}