package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.header;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.jcrom;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.jackrabbit.api.security.user.Group;
import org.jcrom.Jcrom;
import org.junit.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.common.collect.ImmutableMap;

import play.Play;
import play.api.mvc.Call;
import play.libs.F;
import play.mvc.Result;
import play.test.FakeRequest;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class ApplicationTest {

  @Test
  public void indexRedirectsToLoginPage() {
    running(fakeAorraApp(false), new Runnable() {
      @Override
      public void run() {
        {
          Result result = callAction(
              controllers.routes.ref.FileStoreController.index());
          assertThat(status(result)).isEqualTo(303);
          assertThat(header("Location", result)).isEqualTo("/login");
        }
        {
          Result result = callAction(
              controllers.routes.ref.Application.login());
          assertThat(status(result)).isEqualTo(OK);
          assertThat(header("Cache-Control", result)).isEqualTo("max-age=0, must-revalidate");
          String pageContent = contentAsString(result);
          assertThat(pageContent).contains("name=\"email\"");
          assertThat(pageContent).contains("name=\"password\"");
        }
      }
    });
  }

  @Test
  public void loginDetectsInvalidEmailAddress() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", "notanemailaddress");
        data.put("password", "password");
        Result result;
        {
          final FakeRequest request =
              fakeRequest().withFormUrlEncodedBody(data);
          result = callAction(
            controllers.routes.ref.Application.postLogin(),
            request);
        }
        assertThat(status(result)).isEqualTo(400);
        String pageContent = contentAsString(result);
        assertThat(pageContent).contains("name=\"email\"");
        assertThat(pageContent).contains("name=\"password\"");
      }
    });
  }

  @Test
  public void missingUserPopulatesFlashMessage() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", "user@domain.com");
        data.put("password", "password");
        Result result;
        {
          final FakeRequest request =
              fakeRequest().withFormUrlEncodedBody(data);
          result = callAction(
            controllers.routes.ref.Application.postLogin(),
            request);
        }
        assertThat(status(result)).isEqualTo(303);
        assertThat(header("Location", result)).isEqualTo("/login");
        assertThat(flash(result).get("error")).contains("email address or password");
      }
    });
  }

  @Test
  public void incorrectPasswordPopulatesFlashMessage() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final String password = "password";
        final User user = createNewUser("user@domain.com", "differentpassword");
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", user.getEmail());
        data.put("password", password);
        Result result;
        {
          final FakeRequest request =
              fakeRequest().withFormUrlEncodedBody(data);
          result = callAction(
            controllers.routes.ref.Application.postLogin(),
            request);
        }
        assertThat(status(result)).isEqualTo(303);
        assertThat(header("Location", result)).isEqualTo("/login");
        assertThat(flash(result).get("error")).contains("email address or password");
      }
    });
  }

  @Test
  public void unvalidatedUserCannotLogin() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final String password = "password";
        final String email = "user@domain.com";
        createNewUser(email, password);
        final User user = getSessionFactory().inSession(
            new F.Function<Session, User>() {
              @Override
              public User apply(Session session)
                  throws RepositoryException {
                final UserDAO dao = getUserDAO(session);
                final User user = dao.findByEmail(email);
                user.setVerified(false);
                getUserDAO(session).update(user);
                return user;
              };
            });
        assertThat(user.isVerified()).isFalse();
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", user.getEmail());
        data.put("password", password);
        Result result;
        {
          final FakeRequest request =
              fakeRequest().withFormUrlEncodedBody(data);
          result = callAction(
            controllers.routes.ref.Application.postLogin(),
            request);
        }
        assertThat(status(result)).isEqualTo(303);
        try {
          assertThat(header("Location", result))
            .isEqualTo("/user-unverified/" +
                URLEncoder.encode(user.getEmail(),"UTF-8"));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Test
  public void validatedUserCanLogin() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final String password = "password";
        final User user = createNewUser("user@domain.com", password);
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", user.getEmail());
        data.put("password", password);
        Result result;
        {
          final FakeRequest request =
              fakeRequest().withFormUrlEncodedBody(data);
          result = callAction(
            controllers.routes.ref.Application.postLogin(),
            request);
        }
        assertThat(status(result)).isEqualTo(303);
        assertThat(header("Location", result)).isEqualTo("/");
        {
          final FakeRequest request = fakeRequest();
          for (final Map.Entry<String, String> e : session(result).entrySet()) {
            request.withSession(e.getKey(), e.getValue());
          }
          result = callAction(
              controllers.routes.ref.FileStoreController.index(),
              request);
        }
        assertThat(status(result)).isEqualTo(200);
        String pageContent = contentAsString(result);
        assertThat(pageContent).contains(user.getName());
        assertThat(pageContent).contains(user.getEmail());
        assertThat(pageContent).doesNotContain("name=\"password\"");

      }
    });
  }

  @Test
  public void changePassword() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) {
        final UserDAO dao = new UserDAO(session, jcrom());
        {
          final Map<String,String> data = ImmutableMap.<String, String>builder()
            .put("newPassword", "passphrase")
            .build();
          final Result result = callAction(
            controllers.routes.ref.Application.changePassword(),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(400);
          assertThat(dao.checkPassword(user, "passphrase")).isFalse();
        }
        {
          final Map<String,String> data = ImmutableMap.<String, String>builder()
            .put("currentPassword", "notthepassword")
            .put("newPassword", "passphrase")
            .build();
          final Result result = callAction(
            controllers.routes.ref.Application.changePassword(),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(400);
          assertThat(dao.checkPassword(user, "passphrase")).isFalse();
        }
        {
          final Map<String,String> data = ImmutableMap.<String, String>builder()
            .put("currentPassword", "password")
            .put("newPassword", "passphrase")
            .build();
          final Result result = callAction(
            controllers.routes.ref.Application.changePassword(),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(200);
          assertThat(dao.checkPassword(user, "passphrase")).isTrue();
        }
        return session;
      }
    });
  }

  @Test
  public void userCanInviteOthers() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final String inviteUserEmail = "inviteduser@example.test";
        final String inviteUserName = "Invited User";
        {
          final Result result = callAction(
            controllers.routes.ref.Application.invite(),
            newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          final String pageContent = contentAsString(result);
          assertThat(pageContent).contains("name=\"email\"");
          assertThat(pageContent).contains("name=\"name\"");
          assertThat(pageContent).contains("name=\"groups[]\"");
        }
        {
          final Map<String,String> data = new HashMap<String,String>();
          data.put("email", inviteUserEmail);
          data.put("name", inviteUserName);
          data.put("groups[]", "testgroup");
          final Result result = callAction(
            controllers.routes.ref.Application.postInvite(),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(303);
          try {
            assertThat(header("Location", result))
              .isEqualTo("/user-unverified/" +
                  URLEncoder.encode(inviteUserEmail,"UTF-8"));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        // Check the user was created properly
        final UserDAO dao = new UserDAO(session, jcrom());
        final User invitedUser = dao.findByEmail(inviteUserEmail);
        assertThat(invitedUser).isNotNull();
        assertThat(invitedUser.getName()).isEqualTo(inviteUserName);
        assertThat(invitedUser.isVerified()).isFalse();
        final Set<Group> groups = (new GroupManager(session)).memberships(
            invitedUser.getJackrabbitUserId());
        assertThat(groups).hasSize(1);
        for (final Group g : groups) {
          assertThat(g.getPrincipal().getName()).isEqualTo("testgroup");
        }
        // Get the unverified page referred to
        {
          final Map<String,String> data = new HashMap<String,String>();
          data.put("email", inviteUserEmail);
          data.put("name", inviteUserName);
          data.put("groups[]", "testgroup");
          final Result result = callAction(
            controllers.routes.ref.Application.userUnverified(inviteUserEmail),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
        }
        return session;
      }
    });
  }

  @Test
  public void invitesMustProvideName() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Map<String,String> data = new HashMap<String,String>();
        data.put("email", "inviteduser@example.test");
        data.put("name", "");
        final Result result = callAction(
          controllers.routes.ref.Application.postInvite(),
          newRequest.withFormUrlEncodedBody(data));
        assertThat(status(result)).isEqualTo(400);
        return session;
      }
    });
  }

  @Test
  public void passwordReset() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final User user = createNewUser("user@domain.com", "unknownpassword");
        // Clear verification token
        getSessionFactory().inSession(new F.Function<Session, Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final UserDAO dao = getUserDAO(session);
            final User u = dao.findByEmail(user.getEmail());
            u.clearVerificationToken();
            dao.update(u);
            return session;
          }
        });
        assertThat(getToken(user.getEmail())).isNull();
        {
          final Result result = callAction(
            controllers.routes.ref.Application.login(),
            fakeRequest());
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          final Document doc = Jsoup.parse(contentAsString(result));
          final Call resetRoute =
              controllers.routes.Application.forgottenPassword();
          assertThat(doc.select("a[href="+resetRoute.url()+"]"))
            .as("forgotten password link").hasSize(1);
        }
        {
          final Result result = callAction(
            controllers.routes.ref.Application.forgottenPassword(),
            fakeRequest());
          assertThat(status(result)).isEqualTo(200);
          final Document doc = Jsoup.parse(contentAsString(result));
          assertThat(doc.select("input[name=email]"))
            .as("has email field").hasSize(1);
          assertThat(doc.select("input[name=email]").first().attr("type"))
            .isEqualTo("email");
          assertThat(doc.select("button[type=submit]"))
            .as("has submit button").hasSize(1);
          final Call submitRoute =
              controllers.routes.Application.postForgottenPassword();
          assertThat(doc.select("form").first().attr("action"))
            .isEqualTo(submitRoute.url());
          assertThat(doc.select("form").first().attr("method"))
            .isEqualTo(submitRoute.method());
        }
        {
          assertThat(getToken(user.getEmail())).isNull();
          final Map<String,String> data = new HashMap<String,String>();
          data.put("email", user.getEmail());
          final Result result = callAction(
            controllers.routes.ref.Application.postForgottenPassword(),
            fakeRequest().withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(303);
          assertThat(header("Location", result))
            .isEqualTo(controllers.routes.Application.login().url());
          assertThat(flash(result).containsKey("info")).isTrue();
          assertThat(getToken(user.getEmail())).isNotNull();
        }
        {
          final Result result = callAction(
            controllers.routes.ref.Application.verify(user.getEmail(),
                getToken(user.getEmail())),
            fakeRequest());
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          final Document doc = Jsoup.parse(contentAsString(result));
          assertThat(doc.select("input[name=password]"))
            .as("has password field").hasSize(1);
          assertThat(doc.select("input[name=repeatPassword]"))
          .as("has repeat password field").hasSize(1);
        }
        final String token;
        {
          assertThat(checkPwd(user.getEmail(), "newpass")).isFalse();
          final Map<String,String> data = new HashMap<String,String>();
          data.put("password", "newpassword");
          data.put("repeatPassword", "newpassword");
          token = getToken(user.getEmail());
          final Result result = callAction(
            controllers.routes.ref.Application.postVerify(user.getEmail(),
                token),
            fakeRequest().withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(303);
          assertThat(checkPwd(user.getEmail(), "newpassword"))
            .as("password has been reset").isTrue();
          assertThat(getToken(user.getEmail()))
            .as("verification token has been cleared").isNull();
        }
        // Check we cannot reuse the link
        {
          final Result result = callAction(
            controllers.routes.ref.Application.verify(user.getEmail(),
                token),
            fakeRequest());
          assertThat(status(result)).isEqualTo(403);
        }
        {
          final Map<String,String> data = new HashMap<String,String>();
          data.put("password", "otherpass");
          data.put("repeatPassword", "otherpass");
          final Result result = callAction(
            controllers.routes.ref.Application.postVerify(user.getEmail(),
                token),
            fakeRequest().withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(403);
          assertThat(checkPwd(user.getEmail(), "otherpass"))
            .as("password was not reset").isFalse();
        }
      }

      protected boolean checkPwd(final String email, final String password) {
        return getSessionFactory().inSession(new F.Function<Session, Boolean>(){
          @Override
          public Boolean apply(Session session) throws Throwable {
            final UserDAO dao = getUserDAO(session);
            final User u = dao.findByEmail(email);
            dao.update(u);
            return dao.checkPassword(u, password);
          }
        });
      }

      protected String getToken(final String email) {
        return getSessionFactory().inSession(new F.Function<Session, String>() {
          @Override
          public String apply(Session session) throws Throwable {
            final UserDAO dao = getUserDAO(session);
            final User u = dao.findByEmail(email);
            dao.update(u);
            return extractToken(u);
          }

          public String extractToken(User user) {
            try {
              Field vtField = user.getClass()
                  .getDeclaredField("verificationToken");
              vtField.setAccessible(true);
              return (String) vtField.get(user);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      }

    });
  }

  @Test
  public void userCannotInviteThemselves() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) {
        {
          final Map<String,String> data = new HashMap<String,String>();
          data.put("email", user.getEmail());
          data.put("name", user.getName());
          final Result result = callAction(
            controllers.routes.ref.Application.postInvite(),
            newRequest.withFormUrlEncodedBody(data));
          assertThat(status(result)).isEqualTo(303);
          try {
            assertThat(header("Location", result))
              .isEqualTo("/user-exists/" +
                  URLEncoder.encode(user.getEmail(),"UTF-8"));
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
          }
        }
        {
          final Result result = callAction(
              controllers.routes.ref.Application.userExists(user.getEmail()),
              newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
        }
        return session;
      }
    });
  }

  private User createNewUser(final String email, final String password) {
    final String name = "Test User";
    final User.Invite invite = new User.Invite(email, name);
    final JackrabbitEmailPasswordAuthProvider authProvider =
        Play.application().plugin(JackrabbitEmailPasswordAuthProvider.class);
    authProvider.signup(invite);
    User user = getSessionFactory().inSession(new F.Function<Session, User>() {

      @Override
      public User apply(Session session) throws Throwable {
        final UserDAO dao = getUserDAO(session);
        final User user = dao.findByEmail(email);
        user.setVerified(true);
        dao.update(user);
        dao.setPassword(user, password);
        return user;
      }

    });
    return user;
  }

  private JcrSessionFactory getSessionFactory() {
    return GuiceInjectionPlugin.getInjector(Play.application())
        .getInstance(JcrSessionFactory.class);
  }

  private UserDAO getUserDAO(Session session) {
    final Jcrom jcrom = GuiceInjectionPlugin.getInjector(Play.application())
        .getInstance(Jcrom.class);
    return new UserDAO(session, jcrom);
  }


}
