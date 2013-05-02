package models;

import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.junit.Test;

import play.Play;
import play.libs.F.Function;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class UserDAOTest {

  @Test
  public void findByEmail() {

    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = GuiceInjectionPlugin
            .getInjector(Play.application())
            .getInstance(JcrSessionFactory.class);
        final Jcrom jcrom = GuiceInjectionPlugin
            .getInjector(Play.application())
            .getInstance(Jcrom.class);
        sessionFactory.inSession(new Function<Session,UserDAO>() {
          @Override
          public UserDAO apply(Session session) {
            final UserDAO dao = new UserDAO(session, jcrom);
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
            return dao;
          }
        });
      }
    });
  }

}
