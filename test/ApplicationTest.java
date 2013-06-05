import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.flash;
import static play.test.Helpers.header;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;
import static test.AorraTestUtils.fakeAorraApp;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;
import models.UserDAO;

import org.jcrom.Jcrom;
import org.junit.Test;

import play.Play;
import play.libs.F;
import play.mvc.Result;
import play.test.FakeRequest;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class ApplicationTest {

  @Test
	public void indexShowsLoginPage() {
		running(fakeAorraApp(), new Runnable() {
			@Override
			public void run() {
				Result result = callAction(controllers.routes.ref.Application.index());
				assertThat(status(result)).isEqualTo(OK);
				String pageContent = contentAsString(result);
				assertThat(pageContent).contains("name=\"email\"");
        assertThat(pageContent).contains("name=\"password\"");
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
        assertThat(header("Location", result)).isEqualTo("/");
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
        assertThat(header("Location", result)).isEqualTo("/");
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
              controllers.routes.ref.Application.index(),
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
