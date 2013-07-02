package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.fakeRequest;
import static play.test.Helpers.header;
import static play.test.Helpers.running;
import static play.test.Helpers.session;
import static play.test.Helpers.status;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.junit.Test;

import play.Play;
import play.test.FakeRequest;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import providers.JackrabbitEmailPasswordAuthProvider;
import service.filestore.FileStore;
import service.filestore.JsonBuilder;
import service.filestore.roles.Admin;

public class FileStoreControllerTest {

  @Test
  public void getFilestoreJSON() {
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
        assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        final String expectedContent = (new JsonBuilder())
            .toJson(fileStore().getManager(session).getFolders())
            .asText();
        assertThat(contentAsString(result)).contains(expectedContent);
        return session;
      }
    });
  }

  @Test
  public void getFolderHTML() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFolder(
                  fm.getRoot().getIdentifier()),
              newRequest.withHeader("Accept", "text/html"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        }
        return session;
      }
    });
  }

  @Test
  public void getFileHTML() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final FileStore.File file =
            fm.getRoot().createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Some content.".getBytes()));
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFile(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "text/html"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("text/html");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        }
        return session;
      }
    });
  }

  @Test
  public void getFolderJSON() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFolder(
                  fm.getRoot().getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          final String expectedContent = (new JsonBuilder())
              .toJsonShallow(fm.getRoot(), false)
              .asText();
          assertThat(contentAsString(result)).contains(expectedContent);
        }
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFolder(
                  (new UUID(0, 0)).toString()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(404);
        }
        return session;
      }
    });
  }

  @Test
  public void getFileJSON() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFile(
                  fm.getRoot().getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(404);
        }
        {
          final FileStore.File file =
              fm.getRoot().createFile("test.txt", "text/plain",
                  new ByteArrayInputStream("Some content.".getBytes()));
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.showFile(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/json");
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          final String expectedContent = (new JsonBuilder())
              .toJsonShallow(file)
              .asText();
          assertThat(contentAsString(result)).contains(expectedContent);
        }
        return session;
      }
    });
  }

  @Test
  public void uploadToFolder() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        {
          // Updating with correct body
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.uploadToFolder(
                  fm.getRoot().getIdentifier()),
              newRequest.withHeader("Accept", "application/json")
                .withBody(
                    test.AorraScalaHelper.testMultipartFormBody(
                        "Some content.")));
          assertThat(contentAsString(result))
            .isEqualTo("{\"files\":[{\"name\":\"test.txt\",\"size\":13}]}");
          assertThat(status(result)).isEqualTo(200);
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          assertThat(IOUtils.toString(
              ((FileStore.File)
                  fm.getRoot().getFileOrFolder("test.txt")).getData()))
              .isEqualTo("Some content.");
        }
        return session;
      }
    });
  }

  @Test
  public void updateFile() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final FileStore.File file =
            fm.getRoot().createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Some content.".getBytes()));
        {
          // Try without body
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.updateFile(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          // Should return 400 Bad Request
          assertThat(status(result)).isEqualTo(400);
        }
        {
          // Updating with correct body
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.updateFile(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "application/json")
                .withBody(
                    test.AorraScalaHelper.testMultipartFormBody(
                        "New content.")));
          assertThat(contentAsString(result))
            .isEqualTo("{\"files\":[{\"name\":\"test.txt\",\"size\":12}]}");
          assertThat(status(result)).isEqualTo(200);
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          assertThat(IOUtils.toString(
              ((FileStore.File)
                  fm.getByIdentifier(file.getIdentifier())).getData()))
              .isEqualTo("New content.");
        }
        return session;
      }
    });
  }

  @Test
  public void deleteFile() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final FileStore.File file =
            fm.getRoot().createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Some content.".getBytes()));
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.delete(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(204);
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        }
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.delete(
                  file.getIdentifier()),
              newRequest.withHeader("Accept", "application/json"));
          assertThat(status(result)).isEqualTo(404);
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        }
        return session;
      }
    });
  }

  @Test
  public void downloadFile() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final FileStore.File file =
            fm.getRoot().createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Some content.".getBytes()));
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.downloadFile(
                  file.getIdentifier(),
                  "1.0"),
              newRequest.withHeader("Accept", "text/plain"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          assertThat(header("Content-Disposition", result))
            .startsWith("attachment; filename=");
          // File result is async
          assertThat(result.getWrappedResult()).isInstanceOf(
              play.api.mvc.AsyncResult.class);
        }
        return session;
      }
    });
  }

  @Test
  public void fileTextSummary() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final FileStore.File file =
            fm.getRoot().createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Some content.".getBytes()));
        {
          final Result result = callAction(
              controllers.routes.ref.FileStoreController.fileTextSummary(
                  file.getIdentifier(),
                  "1.0"),
              newRequest.withHeader("Accept", "text/plain"));
          assertThat(status(result)).isEqualTo(200);
          assertThat(charset(result)).isEqualTo("utf-8");
          assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
          assertThat(contentAsString(result)).contains("Some content.");
        }
        return session;
      }
    });
  }

  @Test
  public void getGroupJson() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final FileStore.Manager fm = fileStore().getManager(session);
        final Result result = callAction(
            controllers.routes.ref.FileStoreController.groupPermissionList(
                fm.getRoot().getIdentifier()),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        assertThat(contentAsString(result)).contains("testgroup");
        return session;
      }
    });
  }

  @Test
  public void getUsersJson() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.FileStoreController.usersJson(),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result)).isEqualTo("no-cache");
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        json.add((new JsonBuilder()).toJson(user));
        final String expectedContent = json.asText();
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
