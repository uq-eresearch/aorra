package test;

import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.session;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.jcrom.Jcrom;

import play.Application;
import play.Play;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;
import service.filestore.roles.Admin;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;

public class AorraTestUtils {

  public static final String REPOSITORY_CONFIG_PATH = "test/repository.xml";
  public static final String REPOSITORY_DIRECTORY_PATH = "file:./target/jackrabbittestrepository";

  /**
   * Allow multiple tasks to be run during the same application run.
   *
   * @param app   Application to use for tasks
   * @param tasks Runnable tasks to perform
   */
  public static void running(FakeApplication app, final Runnable ... tasks) {
    play.test.Helpers.running(app, new Runnable() {
      @Override
      public void run() {
        for (final Runnable task : tasks) {
          task.run();
        }
      }
    });
  }

  public static FakeApplication fakeAorraApp() {
    return fakeAorraApp(true);
  }

  public static FakeApplication fakeAorraApp(boolean muteErrors) {
    return fakeApplication(additionalConfig(muteErrors));
  }

  private static Map<String, Object> additionalConfig(boolean muteErrors) {
    ImmutableMap.Builder<String, Object> m = ImmutableMap
        .<String, Object> builder();
    if (muteErrors) {
      m.put("logger.play", "ERROR");
      m.put("logger.application", "ERROR");
    }
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_URI, REPOSITORY_DIRECTORY_PATH);
    m.put(ConfigConsts.CONF_JCR_REPOSITORY_CONFIG, REPOSITORY_CONFIG_PATH);
    m.put(ConfigConsts.CONF_JCR_HAS_RECREATION_REQUIRE, true);
    m.put("crash.enabled", false);
    return m.build();
  }

  public static JcrSessionFactory sessionFactory() {
    return injector().getInstance(JcrSessionFactory.class);
  }

  public static FileStore fileStore() {
    return injector().getInstance(FileStoreImpl.class);
  }

  public static Jcrom jcrom() {
    return injector().getInstance(Jcrom.class);
  }

  public static Injector injector() {
    return Play.application().plugin(GuiceInjectionPlugin.class)
        .getInjector();
  }

  // Also used in FileStoreAsyncSpec
  public static void asAdminUser(
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

  private static User createNewUser(final String email, final String password) {
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
  private static FakeRequest loggedInRequest(
      final User user, final String password) {
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