package notifications;

import static org.fest.assertions.Assertions.assertThat;
import static test.AorraTestUtils.absoluteUrl;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.flagStore;
import static test.AorraTestUtils.jcrom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.MessagingException;

import models.Flag;
import models.Notification;
import models.User;
import models.UserDAO;

import org.junit.Test;

import play.libs.F;
import play.test.FakeRequest;
import service.filestore.EventManager;
import service.filestore.EventManager.Event;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.FlagStore.FlagType;
import service.filestore.OrderedEvent;

public class NotificationManagerTest {

  @Test
  public void sendsNotifications() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest newRequest)
          throws RepositoryException, InterruptedException, IOException,
            MessagingException {
        User flaguser = createFlagUser(session);
        final FlagStore.Manager flm = flagStore().getManager(session);
        final FileStore.File f = fileStore().getManager(session)
            .getRoot()
            .createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Test content.".getBytes()));
        // Wait, so we don't end up watching retroactively
        Thread.sleep(100);
        flm.setFlag(FlagType.WATCH, f.getIdentifier(), flaguser);
        // Perform update and trigger event
        f.update("text/plain",
                new ByteArrayInputStream("Test content.".getBytes()));
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(2);
        awaitNotifications(1);
        flaguser = new UserDAO(session, jcrom()).loadById(flaguser.getId());
        final List<Notification> notifications = flaguser.getNotifications();
        assertThat(notifications).hasSize(1);
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(3);
        for (Notification notification : flaguser.getNotifications()) {
          String messageContent = notification.getMessage();
          assertThat(messageContent).isNotNull();
          assertThat(messageContent).contains("/test.txt");
          assertThat(messageContent).contains(
              absoluteUrl(controllers.routes.FileStoreController.showFile(
                  f.getIdentifier())));
        }
        return session;
      }
    });
  }

  @Test
  public void sendsEditingNotifications() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest newRequest)
          throws RepositoryException, InterruptedException, IOException,
            MessagingException {
        User flaguser = createFlagUser(session);
        final FlagStore.Manager flm = flagStore().getManager(session);
        final FileStore.File f = fileStore().getManager(session)
            .getRoot()
            .createFile("test.txt", "text/plain",
                new ByteArrayInputStream("Test content.".getBytes()));
        // Wait, so we don't end up watching retroactively
        Thread.sleep(100);
        flm.setFlag(FlagType.WATCH, f.getIdentifier(), flaguser);
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(1);
        // Perform set flag and trigger event
        final Flag flag = flm.setFlag(FlagType.EDIT, f.getIdentifier(), user);
        fileStore().getEventManager().tell(Event.create(flag));
        awaitNotifications(1);
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(3);
        flaguser =  (new UserDAO(session, jcrom())).loadById(flaguser.getId());
        assertThat(flaguser.getNotifications()).hasSize(1);
        Notification notification = flaguser.getNotifications().get(0);
        String messageContent = notification.getMessage();
        assertThat(messageContent).isNotNull();
        assertThat(messageContent).contains("editing");
        assertThat(messageContent).contains("/test.txt");
        assertThat(messageContent).contains(
            absoluteUrl(controllers.routes.FileStoreController.showFile(
                f.getIdentifier())));
        return session;
      }
    });
  }

  private User createFlagUser(Session session) {
    final User u = new User();
    u.setEmail("flaguser@flagtest.test");
    u.setName("Flag User");
    return (new UserDAO(session, jcrom())).create(u);
  }

  public static void awaitNotifications(int count) {
    for (int retries = 50; retries > 0; retries--) {
      try {
        Iterable<OrderedEvent> events =
            fileStore().getEventManager().getSince(null);
        int actualCount = 0;
        for(OrderedEvent oe : events) {
          EventManager.Event event = oe.event();
          if (event.info != null &&
              event.info.type == EventManager.Event.NodeType.NOTIFICATION) {
            actualCount++;
          }
        }
        if (actualCount >= count)
          return;
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    throw new RuntimeException(String.format(
        "Time out waiting for %s notifications.", count));
  }

}
