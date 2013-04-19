package service.filestore.roles;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.*;

import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import static test.AorraTestUtils.fakeAorraApp;
import play.Play;
import play.libs.F.Function;

import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;

/**
 * Tests for the Admin role.
 *
 * @author Tim Dettrick <t.dettrick@uq.edu.au>
 *
 */
public class AdminTest {

  @Test
  public void canGetInstance() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        JcrSessionFactory sessionFactory = GuiceInjectionPlugin
            .getInjector(Play.application())
            .getInstance(JcrSessionFactory.class);
        sessionFactory.inSession(new Function<Session,Admin>() {
          @Override
          public Admin apply(Session session) {
            Admin admin = Admin.getInstance(session);
            assertThat(admin).isNotNull();
            assertThat(admin.getGroup()).isNotNull();
            try {
              assertThat(admin.getGroup().getID()).isEqualTo(Admin.GROUP_ID);
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
            return admin;
          }
        });
      }
    });
  }

}