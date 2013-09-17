package service;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import play.libs.F;

public class JcrSessionFactoryTest {

  private static TransientRepository repo;

  @BeforeClass
  public static void setUp() {
    repo = new TransientRepository(new File("./test"));
  }

  @AfterClass
  public static void tearDown() {
    repo.shutdown();
  }

  @Test
  public void testNewUserSession() throws LoginException, RepositoryException {
    final JcrSessionFactory sf = getSessionFactory();
    Session session = null;
    try {
      session = sf.newUserSession(
          new SimpleCredentials("anonymous", new char[0]));
      assertThat(session.isLive()).isTrue();
      assertThat(session.getUserID()).isEqualTo("anonymous");
    } finally {
      if (session != null) session.logout();
    }
    try {
      sf.newUserSession(new SimpleCredentials("doesnotexist", new char[0]));
      fail("Should not succeed.");
    } catch (RuntimeException re) {
      assertThat(re.getCause()).isInstanceOf(LoginException.class);
    }
  }

  @Test
  public void testInSessionFunctionOfSessionR() {
    final JcrSessionFactory sf = getSessionFactory();
    final Session session = sf.inSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) {
        assertThat(session.isLive()).isTrue();
        assertThat(session.getUserID()).isEqualTo("admin");
        return session;
      }
    });
    assertThat(session.isLive()).isFalse();
  }

  @Test
  public void testInSessionEarlyEnd() {
    final JcrSessionFactory sf = getSessionFactory();
    // Check we can close the session early
    sf.inSession(new F.Function<Session, Session>() {
      @Override
      public Session apply(Session session) throws RepositoryException {
        assertThat(session.isLive()).isTrue();
        session.logout();
        assertThat(session.isLive()).isFalse();
        return session;
      }
    });
    // Check we handle thrown exceptions
    final List<Session> sessions = Lists.newLinkedList();
    try {
      sf.inSession(new F.Function<Session, Session>() {
        @Override
        public Session apply(Session s) throws Throwable {
          sessions.add(s);
          assertThat(s.isLive()).isTrue();
          throw new Throwable() {
            private static final long serialVersionUID = 1L;
          };
        }
      });
      fail("Should have raised exception.");
    } catch (Throwable t) {
      assertThat(sessions).hasSize(1);
      assertThat(sessions.get(0).isLive()).isFalse();
    }
  }

  @Test
  public void testInSessionStringFunctionOfSessionR() {
    final JcrSessionFactory sf = getSessionFactory();
    final Session session = sf.inSession("anonymous",
        new F.Function<Session, Session>() {
      @Override
      public Session apply(final Session s) {
        assertThat(s.isLive()).isTrue();
        assertThat(s.getUserID()).isEqualTo("anonymous");
        return s;
      }
    });
    assertThat(session.isLive()).isFalse();
  }

  @Test
  public void testInSessionCredentialsFunctionOfSessionR()
      throws NoSuchAlgorithmException, UnsupportedEncodingException {
    final JcrSessionFactory sf = getSessionFactory();
    {
      final Session session = sf.inSession(
          new SimpleCredentials("anonymous", new char[0]),
          new F.Function<Session, Session>() {
        @Override
        public Session apply(final Session s) {
          assertThat(s.isLive()).isTrue();
          assertThat(s.getUserID()).isEqualTo("anonymous");
          return s;
        }
      });
      assertThat(session.isLive()).isFalse();
    }
    {
      final Session session = sf.inSession(
          new CryptedSimpleCredentials("anonymous", ""),
          new F.Function<Session, Session>() {
        @Override
        public Session apply(final Session s) {
          assertThat(s.isLive()).isTrue();
          assertThat(s.getUserID()).isEqualTo("anonymous");
          return s;
        }
      });
      assertThat(session.isLive()).isFalse();
    }
    try{
      sf.inSession(
          // Useless credentials
          new Credentials() {
            private static final long serialVersionUID = 1L;
          },
          new F.Function<Session, Session>() {
        @Override
        public Session apply(final Session s) {
          fail("Should not get here.");
          return s;
        }
      });
      fail("Should fail with exception.");
    } catch (RuntimeException e) {
      // Expected
    }
  }

  private JcrSessionFactory getSessionFactory() {
    final Credentials adminCredentials = new SimpleCredentials(
        "admin", "admin".toCharArray());
    return new JcrSessionFactory() {
      @Override
      public Session newAdminSession() {
        final Session session;
        try {
          session = repo.login(adminCredentials);
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }
        assertThat(session.isLive()).isTrue();
        return session;
      }
    };
  }

}
