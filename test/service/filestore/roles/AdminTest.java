package service.filestore.roles;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.*;

import service.JcrSessionFactory;
import static test.AorraTestUtils.fakeJavaApp;
import static test.AorraTestUtils.injector;
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
    running(fakeJavaApp(), new Runnable() {
      public void run() {
        JcrSessionFactory sessionFactory = 
            injector().getInstance(JcrSessionFactory.class);
        sessionFactory.inSession(new Function<Session,Admin>() {
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