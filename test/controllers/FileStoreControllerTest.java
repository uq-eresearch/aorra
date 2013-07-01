package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.junit.Test;

import play.Play;
import play.test.FakeRequest;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.filestore.JsonBuilder;
import service.filestore.roles.Admin;

public class FileStoreControllerTest {

  @Test
  public void callFolderJSON() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.FileStoreController.filestoreJson(),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        final String expectedContent = (new JsonBuilder())
            .toJson(fileStore().getManager(session).getFolders())
            .asText();
        assertThat(contentAsString(result)).contains(expectedContent);
        return session;
      }
    });
  }

  private void asAdminUser(
      final F.Function3<Session, User, FakeRequest, Session> op) {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(final Session session) throws Throwable {
            final String password = "password";
            final User user = createNewUser("test@example.com", password);
            final GroupManager gm = new GroupManager(session);
            // Create admin group

            Admin.getInstance(session).getGroup().addMember(
                gm.create("testgroup"));
            gm.addMember("testgroup", user.getJackrabbitUserId());
            // Do op
            op.apply(session, user, loggedInRequest(user, password));
            return session;
          }
        });
      }
    });
  }

  private User createNewUser(final String email, final String password) {
    final String name = "Test User";
    final User.Invite invite = new User.Invite(email, name);
    final JackrabbitEmailPasswordAuthProvider authProvider =
        Play.application().plugin(JackrabbitEmailPasswordAuthProvider.class);
    authProvider.signup(invite);
    User user = sessionFactory().inSession(new F.Function<Session, User>() {

      @Override
      public User apply(Session session) throws Throwable {
        final UserDAO dao = new UserDAO(session, jcrom());
        final User user = dao.findByEmail(email);
        user.setVerified(true);
        dao.update(user);
        dao.setPassword(user, password);
        return user;
      }

    });
    return user;
  }

  /*
   * Must be used while application is running.
   */
  private FakeRequest loggedInRequest(final User user, final String password) {
    final Map<String,String> data = new HashMap<String,String>();
    data.put("email", user.getEmail());
    data.put("password", password);
    final Result result = callAction(
        controllers.routes.ref.Application.postLogin(),
        fakeRequest().withFormUrlEncodedBody(data));
    FakeRequest newRequest = fakeRequest();
    final Http.Session session = session(result);
    for (Map.Entry<String, String> e : session.entrySet()) {
      newRequest = newRequest.withSession(e.getKey(), e.getValue());
    }
    return newRequest;
  }

}
