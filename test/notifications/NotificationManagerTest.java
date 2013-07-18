package notifications;

import static org.fest.assertions.Assertions.*;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.flagStore;
import static test.AorraTestUtils.mailServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.mail.MessagingException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;

import models.User;
import models.UserDAO;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import play.libs.F;
import play.test.FakeRequest;
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
        final User flaguser = createFlagUser(session);
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
        final MimeMessage message = awaitEmail("flagtest.test");
        assertThat(message).isNotNull();
        assertThat(message.getRecipients(RecipientType.TO)[0].toString())
          .contains("flaguser@flagtest.test");
        //assertThat(message.getRecipients(RecipientType.TO)[0].toString())
        //  .isEqualTo("Flag User <flaguser@flagtest.test>");
        String messageContent = IOUtils.toString(message.getInputStream());
        assertThat(messageContent).contains("/test.txt");
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

  private MimeMessage awaitEmail(String domain) {
    MimeMessage[] messages = null;
    int retries = 30;
    try {
      while (true) {
        mailServer().waitForIncomingEmail(1);
        messages = mailServer().getReceviedMessagesForDomain(domain);
        if (messages.length > 0 || retries-- <= 0)
          break;
        Thread.sleep(1000);
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (messages.length == 0)
      return null;
    return messages[messages.length-1];
  }

}
