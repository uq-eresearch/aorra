package notifications;

import static org.fest.assertions.Assertions.assertThat;
import static test.AorraTestUtils.absoluteUrl;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.flagStore;
import static test.AorraTestUtils.jcrom;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
import scala.Tuple2;
import service.filestore.EventManager;
import service.filestore.EventManager.Event;
import service.filestore.FileStore;
import service.filestore.FlagStore;
import service.filestore.FlagStore.FlagType;

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
        flm.setFlag(FlagType.WATCH, f.getIdentifier(), flaguser);
        // Perform update and trigger event
        f.update("text/plain",
                new ByteArrayInputStream("Test content.".getBytes()));
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(2);
        awaitNotification();
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(3);
        flaguser =  (new UserDAO(session, jcrom())).loadById(flaguser.getId());
        assertThat(flaguser.getNotifications()).hasSize(1);
        Notification notification = flaguser.getNotifications().get(0);
        String messageContent = notification.getMessage();
        assertThat(messageContent).isNotNull();
        assertThat(messageContent).contains("/test.txt");
        assertThat(messageContent).contains(
            absoluteUrl(controllers.routes.FileStoreController.showFile(
                f.getIdentifier())));
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
        flm.setFlag(FlagType.WATCH, f.getIdentifier(), flaguser);
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(1);
        // Perform set flag and trigger event
        final Flag flag = flm.setFlag(FlagType.EDIT, f.getIdentifier(), user);
        fileStore().getEventManager().tell(Event.create(flag));
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(2);
        awaitNotification();
        assertThat(fileStore().getEventManager().getSince(null)).hasSize(3);
        flaguser =  (new UserDAO(session, jcrom())).loadById(flaguser.getId());
        assertThat(flaguser.getNotifications()).hasSize(1);
        Notification notification = flaguser.getNotifications().get(0);
        String messageContent = notification.getMessage();
        assertThat(messageContent).isNotNull();
        assertThat(messageContent).contains("editing the file");
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

  public static void awaitNotification() {
    int retries = 50;
    try {
      while (true) {
        if (retries-- <= 0) {
            throw new RuntimeException("time out waiting for notification");
        }
        Iterable<Tuple2<String, Event>> events = fileStore().getEventManager().getSince(null);
        for(Tuple2<String, Event> pair : events) {
            EventManager.Event event = pair._2;
            if(event.info!=null && event.info.type == EventManager.Event.NodeType.NOTIFICATION) {
                return;
            }
        }
        Thread.sleep(100);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
