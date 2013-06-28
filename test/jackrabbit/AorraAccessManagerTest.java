package jackrabbit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.sessionFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import play.libs.F;

public class AorraAccessManagerTest {

  @Test
  public void testCanAccessAllWorkspaces() {
    acmTest(new F.Function2<Session, AorraAccessManager, Session>() {
      @Override
      public Session apply(Session session, AorraAccessManager acm)
          throws RepositoryException {
        assertTrue(acm.canAccess("default"));
        final String randomWorkspaceName = RandomStringUtils.random(30);
        assertTrue(acm.canAccess(randomWorkspaceName));
        return session;
      }
    });
  }

  /*
   * Lots of unimplemented methods. Let's be explicit which are intended to be.
   */
  @Test
  public void testUnimplementedMethods() {
    acmTest(new F.Function2<Session, AorraAccessManager, Session>() {

      @Override
      public Session apply(Session session, AorraAccessManager acm)
          throws RepositoryException {

        try {
          acm.getApplicablePolicies("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.getEffectivePolicies("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }


        try {
          acm.getSupportedPrivileges("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.getPolicies("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.getPrivileges("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.hasPrivileges("anything", null);
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.privilegeFromName("anything");
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.removePolicy("anything", null);
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        try {
          acm.setPolicy("anything", null);
          fail("Should have thrown NotImplementedException");
        } catch (NotImplementedException e) {
          // All good
        }

        return session;
      }

    });
  }

  private void acmTest(
      final F.Function2<Session, AorraAccessManager, Session> f) {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session, Session>() {

          @Override
          public Session apply(Session session) throws Throwable {
            final AorraAccessManager acm = (AorraAccessManager)
                session.getAccessControlManager();
            return f.apply(session, acm);
          }
        });
      }
    });
  }


}
