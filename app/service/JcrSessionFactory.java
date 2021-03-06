package service;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import
  org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;

import play.libs.F.Function;

public abstract class JcrSessionFactory {

  /**
   * Get a new admin session.
   * @returns a new admin session
   */
  public abstract Session newAdminSession();

  /**
   * Get a new session as the Jackrabbit user matching the given credentials.
   * @param credentials Jackrabbit user credentials
   * @returns a new session as the provided user
   */
  public Session newUserSession(final Credentials credentials) {
    try {
      return impersonate(newAdminSession(), credentials);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform the given function in an admin session.
   * @param a function to perform in the admin session
   * @throws RepositoryException
   * @returns return value of the function
   */
  public <R> R inSession(Function<Session, R> func) {
    return inSession(newAdminSession(), func);
  }

  /**
   * Perform the given function in a session with the provided jackrabbit
   * user ID. The ID will be used to produce credentials for a Jackrabbit User,
   * which will be used for impersonation of a login.
   *
   * Changes will be saved on successful return.
   *
   * @param credentials the Jackrabbit credentials to use for impersonation
   * @param func a function to perform in the user session
   * @returns return value of the function
   */
  public <R> R inSession(String userId, Function<Session, R> func) {
    return inSession(new SimpleCredentials(userId, new char[0]), func);
  }

  /**
   * Perform the given function in a session with the provided credentials.
   * The credentials should come from a Jackrabbit User, and will be used for
   * impersonation of a login. Changes will be saved on successful return.
   *
   * @param credentials the Jackrabbit credentials to use for impersonation
   * @param func a function to perform in the user session
   * @returns return value of the function
   */
  public <R> R inSession(Credentials credentials, Function<Session, R> func) {
    return inSession(newUserSession(credentials), func);
  }

  /**
   * Perform the given function in a session, then close it after saving any
   * pending changes.
   *
   * @param session the JCR session to use
   * @param func a function to perform in the session
   * @throws RuntimeException wraps any exception that may have been thrown
   * @returns return value of the function
   */
  protected <R> R inSession(Session session, Function<Session, R> func) {
    try {
      R result = func.apply(session);
      if (session.isLive() && session.hasPendingChanges()) {
        session.save();
      }
      return result;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    } finally {
      if (session.isLive()) {
        session.logout();
      }
    }
  }

  /**
   * Get session impersonating this user.
   */
  protected Session impersonate(Session session, Credentials creds)
      throws LoginException, RepositoryException {
    Credentials usableCreds;
    {
      if (creds instanceof CryptedSimpleCredentials) {
        usableCreds = new SimpleCredentials(
            ((CryptedSimpleCredentials) creds).getUserID(), new char[0]);
      } else if (creds instanceof SimpleCredentials) {
        usableCreds = creds;
      } else {
        throw new RuntimeException(
            "You can't impersonate with those credentials.");
      }
    }
    return session.impersonate(usableCreds);
  }
}
