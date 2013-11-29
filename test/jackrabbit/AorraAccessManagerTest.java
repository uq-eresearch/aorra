package jackrabbit;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.sessionFactory;

import java.security.AccessControlException;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.security.principal.PrincipalImpl;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.name.PathBuilder;
import org.fest.assertions.MapAssert;
import org.junit.Test;

import play.libs.F;

public class AorraAccessManagerTest {

  @Test
  public void testCanAccessAllWorkspaces() {
    running(fakeAorraApp(),
      acmTest(new F.Function2<Session, AorraAccessManager, Session>() {
        @Override
        public Session apply(Session session, AorraAccessManager acm)
            throws RepositoryException {
          assertTrue(acm.canAccess("default"));
          final String randomWorkspaceName = RandomStringUtils.random(30);
          assertTrue(acm.canAccess(randomWorkspaceName));
          return session;
        }
      })
    );
  }

  @Test
  public void testGetIdAndPath() {
    running(fakeAorraApp(),
      acmTest(new F.Function2<Session, AorraAccessManager, Session>() {
        @Override
        public Session apply(Session session, AorraAccessManager acm)
            throws RepositoryException {
          String rootUid = session.getNode("/").getIdentifier();
          assertThat(rootUid).isEqualTo(acm.getId("/"));
          assertThat("/").isEqualTo(acm.getPath(rootUid));
          try {
            acm.getPath("");
            fail("Should throw IllegalArgumentException.");
          } catch (IllegalArgumentException e) {
            // Good
          }
          return session;
        }
      })
    );
  }

  @Test
  public void testPermissionHandling() throws MalformedPathException {
    final String anon = "anonymous";
    final Path rootPath;
    {
      PathBuilder pb = new PathBuilder();
      pb.addRoot();
      rootPath = pb.getPath();
    }
    running(fakeAorraApp(),
      acmTest(new F.Function2<Session, AorraAccessManager, Session>() {
        @Override
        public Session apply(final Session session, final AorraAccessManager acm)
            throws RepositoryException {
          // Check admin permissions
          final ItemId rootID;
          try {
            acm.checkPermission(rootPath, 3);
            rootID = makeId(session.getNode("/").getIdentifier());
            // Wrapped to avoid suppressing deprecation warning anywhere else
            (new F.Callback0() {
              @SuppressWarnings("deprecation")
              @Override
              public void invoke() throws RepositoryException {
                acm.checkPermission(rootID, 3);
              }
            }).invoke();
          } catch (AccessDeniedException e) {
            fail("Should be allowed.");
            return session;
          }
          // Run some tests on granting
          final MapAssert.Entry expectedPermission = entry(
              new PermissionKey("default", anon, rootID.toString()),
              Permission.RO);
          acm.grant(new PrincipalImpl(anon), "/", Permission.RO);
          assertThat(acm.getPermissions()).includes(expectedPermission);
          acm.revoke("default", anon, rootID.toString());
          assertThat(acm.getPermissions()).excludes(expectedPermission);
          acm.grant(new PrincipalImpl(anon), "/", Permission.RO);
          acm.grant("default", anon, rootID.toString(), Permission.RO);
          assertThat(acm.getPermissions()).includes(expectedPermission);
          try {
            acm.grant(new PrincipalImpl(anon), "/doesnotexist", Permission.RO);
            fail("Should throw ItemNotFoundException.");
          } catch (ItemNotFoundException e) {
            // Good
          }

          return session;
        }
      }),
      acmTest(anon, new F.Function2<Session, AorraAccessManager, Session>() {
        @Override
        public Session apply(final Session session, AorraAccessManager acm)
            throws RepositoryException {
          try {
            acm.checkPermission(rootPath, 1);
          } catch (AccessControlException e) {
            fail("Should be allowed.");
          }
          try {
            acm.checkPermission(rootPath, 3);
            fail("Should not be allowed.");
          } catch (AccessDeniedException e) {
            // All good
          }
          return session;
        }
      })
    );
  }

  /*
   * Lots of unimplemented methods. Let's be explicit which are intended to be.
   */
  @Test
  public void testUnimplementedMethods() {
    running(fakeAorraApp(),
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

    }));
  }

  private Runnable acmTest(
      final F.Function2<Session, AorraAccessManager, Session> f) {
    return acmTest(null, f);
  }

  private Runnable acmTest(final String userId,
      final F.Function2<Session, AorraAccessManager, Session> f) {
    return new Runnable() {
      @Override
      public void run() {
        if (userId == null)
          sessionFactory().inSession(execute(f));
        else
          sessionFactory().inSession(userId, execute(f));
      }
    };
  }

  private F.Function<Session, Session> execute(
      final F.Function2<Session, AorraAccessManager, Session> f) {
    return new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) throws Throwable {
        final AorraAccessManager acm = (AorraAccessManager)
            session.getAccessControlManager();
        return f.apply(session, acm);
      }
    };
  }

  private ItemId makeId(String id) {
    try {
        return new NodeId(id);
    } catch(IllegalArgumentException e) {
        return PropertyId.valueOf(id);
    }
  }

}
