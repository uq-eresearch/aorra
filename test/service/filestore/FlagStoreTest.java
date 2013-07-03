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

import models.Flag;
import models.GroupManager;
import models.User;
import models.UserDAO;

import org.junit.Test;

import play.libs.F.Function;
import service.filestore.FlagStore.FlagType;
import service.filestore.roles.Admin;

public class FlagStoreTest {

  @Test
  public void canAddAndRemoveFlags() {
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

            // Check initial state
            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(0);
            // Create flag
            final Flag flag =
                flm.setFlag(FlagType.WATCH, file.getIdentifier(), user);
            // Check the flag exists in the collection
            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(1);
            assertThat(flm.getFlags(FlagType.WATCH)).contains(flag);
            assertThat(flm.getFlags(FlagType.EDIT)).hasSize(0);
            // Try getting the single flag
            assertThat(flm.getFlag(FlagType.WATCH, flag.getId()))
              .isEqualTo(flag);
            assertThat(flm.getFlag(FlagType.EDIT, flag.getId())).isNull();
            // Try adding it again (should have no effect)
            flm.setFlag(FlagType.WATCH, file.getIdentifier(), user);
            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(1);
            // Remove the flag
            flm.unsetFlag(FlagType.WATCH, flag.getId());
            assertThat(flm.getFlags(FlagType.WATCH)).hasSize(0);
            // Deleting missing ID doesn't throw an exception
            flm.unsetFlag(FlagType.WATCH, flag.getId());

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
