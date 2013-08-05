package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.charset;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static notifications.NotificationManagerTest.awaitNotification;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.flagStore;
import static test.AorraTestUtils.jcrom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.MessagingException;

import models.Flag;
import models.Notification;
import models.User;
import models.UserDAO;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.junit.Test;

import play.api.mvc.Call;
import play.libs.F;
import play.mvc.Result;
import play.test.FakeRequest;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.JsonBuilder;
import service.filestore.EventManager.Event;
import service.filestore.FlagStore.FlagType;

public class UserControllerTest {

  @Test
  public void routes() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        {
          final Call call =
              controllers.routes.UserController.usersJson();
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/user");
        }
        {
          final Call call =
              controllers.routes.UserController.notificationsJson();
          assertThat(call.method()).isEqualTo("GET");
          assertThat(call.url()).isEqualTo("/user/notifications");
        }
        return session;
      }
    });
  }

  @Test
  public void usersJson() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final Result result = callAction(
            controllers.routes.ref.UserController.usersJson(),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        json.add((new JsonBuilder()).toJson(user, true));
        final String expectedContent = json.toString();
        assertThat(contentAsString(result)).contains(expectedContent);
        return session;
      }
    });
  }

  @Test
  public void notificationsJson() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(
          final Session session,
          final User user,
          final FakeRequest newRequest) throws Throwable {
        final User other = createOtherUser(session);
        final FlagStore.Manager flm = flagStore().getManager(session);
        final FileStore.File f = fileStore().getManager(session)
            .getRoot()
            .createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Test content.".getBytes()));
        flm.setFlag(FlagType.WATCH, f.getIdentifier(), user);
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(1);
        // Perform set flag and trigger event
        final Flag flag = flm.setFlag(FlagType.EDIT, f.getIdentifier(), other);
        fileStore().getEventManager().tell(Event.create(flag));
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(2);
        awaitNotification();
        final List<Notification> notifications = (new UserDAO(session, jcrom()))
            .loadById(user.getId()).getNotifications();
        assertThat(notifications).hasSize(1);
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        json.add((new JsonBuilder()).toJson(notifications.get(0)));
        final String expectedContent = json.toString();
        // Fetch notifications
        final Result result = callAction(
            controllers.routes.ref.UserController.notificationsJson(),
            newRequest);
        assertThat(status(result)).isEqualTo(200);
        assertThat(contentType(result)).isEqualTo("application/json");
        assertThat(charset(result)).isEqualTo("utf-8");
        assertThat(header("Cache-Control", result))
          .isEqualTo("max-age=0, must-revalidate");
        assertThat(contentAsString(result)).isEqualTo(expectedContent);
        return session;
      }
    });
  }

  private User createOtherUser(Session session) {
    final User u = new User();
    u.setEmail("flaguser@flagtest.test");
    u.setName("Flag User");
    return (new UserDAO(session, jcrom())).create(u);
  }

}
