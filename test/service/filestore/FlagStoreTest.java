package service.filestore;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.injector;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.junit.Test;

import play.libs.F.Function;
import service.filestore.FlagStore.FlagType;
import service.filestore.roles.Admin;

public class FlagStoreTest {

  @Test
  public void canCreateFlags() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final FlagStore fl = injector().getInstance(FlagStore.class);
        sessionFactory().inSession(new Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws RepositoryException {
            final User user = getAdminUser(session);
            final FileStore.Manager fm = fileStore().getManager(session);
            final FlagStore.Manager flm = fl.getManager(session);
            final FileStore.File file = fm.getRoot().createFile(
                "test.txt", "text/plain",
                new ByteArrayInputStream("Some content".getBytes()));

            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(0);
            flm.setFlag(FlagType.WATCH, file.getIdentifier(), user);
            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(1);

            return session;
          }
        });
      }
    });
  }

  private User getAdminUser(final Session session) throws RepositoryException {
    // Create new user
    final String userId;
    UserDAO dao = new UserDAO(session, jcrom());
    User user = new User();
    user.setEmail("user@example.com");
    user.setName("Test User");
    user = dao.create(user);
    String token = user.createVerificationToken();
    user.checkVerificationToken(token);
    dao.setPassword(user, "password");
    userId = user.getJackrabbitUserId();
    final GroupManager gm = new GroupManager(session);
    Admin.getInstance(session).getGroup().addMember(gm.create("testAdmin"));
    gm.addMember("testAdmin", userId);
    return user;
  }

}
