package models;

import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.junit.Test;

import play.Play;
import play.libs.F;
import play.test.FakeRequest;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class UserDAOTest {

  @Test
  public void findByEmail() {

    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,UserDAO>() {
          @Override
          public UserDAO apply(Session session) {
            final UserDAO dao = new UserDAO(session, jcrom());
            List<String> emailAddresses = Arrays.asList(
                "user@domain.com",
                "user@domain.net",
                "user@uq.edu.au",
                "tommy@domain.com");
            Map<String, User> users = new HashMap<String, User>();
            for (String email : emailAddresses) {
              User u = new User();
              u.setEmail(email);
              u.setName("Sparticus");
              dao.create(u);
              users.put(email, u);
            }
            assertThat(users.size()).isEqualTo(emailAddresses.size());
            for (String email : emailAddresses) {
              User a = dao.findByEmail(email);
              assertThat(a.getEmail()).isEqualTo(email);
              User e = users.get(email);
              assertThat(a.getId()).isEqualTo(e.getId());
            }
            // No need to clean up, but we will to test user deletion
            for (User user : users.values()) {
              dao.delete(user);
              assertThat(dao.findByEmail(user.getEmail())).isNull();
            }
            return dao;
          }
        });
      }
    });
  }

  @Test
  public void suspendUser() {
    asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
      @Override
      public Session apply(Session session, User user, FakeRequest r)
          throws Throwable {
        final UserDAO dao = new UserDAO(session, jcrom());
        assertThat(dao.checkPassword(user, "password")).isTrue();
        dao.suspend(user);
        user = dao.loadById(user.getId());
        assertThat(user.isVerified()).isFalse();
        assertThat(dao.checkPassword(user, "password")).isFalse();
        dao.unsuspend(user);
        user = dao.loadById(user.getId());
        assertThat(user.isVerified()).isTrue();
        assertThat(dao.checkPassword(user, "password")).isTrue();
        return session;
      }
    });
  }

}
