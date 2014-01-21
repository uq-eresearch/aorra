package test;

import static com.google.common.collect.Lists.newArrayList;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.session;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.jcrom.Jcrom;

import play.Application;
import play.Play;
import play.api.mvc.Call;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeApplication;
import play.test.FakeRequest;
import play.test.Helpers;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl;
import service.filestore.FlagStore;
import service.filestore.roles.Admin;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.icegreen.greenmail.util.GreenMail;
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
    final List<String> additionalPlugins = newArrayList(
        "test.FakeMailPlugin");
    final List<String> withoutPlugins = newArrayList(
        "com.typesafe.plugin.CommonsMailerPlugin");
    return new FakeApplication(
        new java.io.File("."), Helpers.class.getClassLoader(),
        additionalConfig(muteErrors),
        additionalPlugins,
        withoutPlugins,
        null);
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
    m.put("notifications.waitMillis", 100L);
    return m.build();
  }

  public static FakeMailPlugin mailServer() {
    return Play.application().plugin(FakeMailPlugin.class);
  }

  public static JcrSessionFactory sessionFactory() {
    return injector().getInstance(JcrSessionFactory.class);
  }

  public static FileStore fileStore() {
    return injector().getInstance(FileStore.class);
  }

  public static FlagStore flagStore() {
    return injector().getInstance(FlagStore.class);
  }

  public static Jcrom jcrom() {
    return injector().getInstance(Jcrom.class);
  }

  public static Injector injector() {
    return Play.application().plugin(GuiceInjectionPlugin.class)
        .getInjector();
  }

  public static String absoluteUrl(final Call call) {
    try {
      URL baseUrl = new URL(Play.application().configuration()
          .getString("application.baseUrl"));
      return (new URL(baseUrl, call.url())).toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  // Also used in FileStoreAsyncSpec
  public static void asAdminUser(
      final F.Function3<Session, User, FakeRequest, Session> op) {
    asAdminUserSession(
        new F.Function3<Session, User, Http.Session, Session>() {
      @Override
      public Session apply(Session session, User user, Http.Session httpSession)
          throws Throwable {
        return op.apply(session, user,
            loggedInRequest(fakeRequest(), httpSession));
      }
    });
  }

  // Also used in FileStoreAsyncSpec
  public static void asAdminUserSession(
      final F.Function3<Session, User, Http.Session, Session> op) {
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
            return op.apply(session, user, loggedInSession(user, password));
          }
        });
      }
    });
  }

  public static FakeRequest loggedInRequest(
      FakeRequest newRequest,
      Http.Session httpSession) {
    for (Map.Entry<String, String> e : httpSession.entrySet()) {
      newRequest = newRequest.withSession(e.getKey(), e.getValue());
    }
    return newRequest;
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
  private static Http.Session loggedInSession(
      final User user, final String password) {
    final Map<String,String> data = new HashMap<String,String>();
    data.put("email", user.getEmail());
    data.put("password", password);
    final Result result = callAction(
        controllers.routes.ref.Application.postLogin(),
        fakeRequest().withFormUrlEncodedBody(data));
    return session(result);
  }

}