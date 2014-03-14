package models;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

import play.Logger;

public class UserDAO extends AbstractJcrDAO<User> {

  public static final String USER_PATH = "/user";

  public final JackrabbitSession session;

  public UserDAO(Session session, Jcrom jcrom) {
    super(User.class, session, jcrom);
    this.session = (JackrabbitSession) session;
  }

  public User get(CacheableUser cachedUser) {
    return loadById(cachedUser.getId());
  }

  @Override
  public User create(User user) {
    return create(getRootPath(), user);
  }

  @Override
  public User create(final String path, final User user) {
    final User createdUser = super.create(path, user);
    try {
      session.getUserManager().createUser(jackrabbitAuthUserId(createdUser),"");
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return createdUser;
  }

  public User findByEmail(String email) {
    for(User u : list()) {
      if(StringUtils.equalsIgnoreCase(email, u.getEmail())) {
        return u;
      }
    }
    return null;
    // issue #158, the following code compares emails case sensitive
    // but they are case insensitive (in practice)
//    final String nodeName = User.generateNodeName(email);
//    return get(USER_PATH+"/"+nodeName);
  }

  public List<User> list() {
    return findAll(getRootPath());
  }

  public void delete(User user) {
    remove(user.getNodePath());
  }

  /**
   * Removes verification without changing the password.
   *
   * @param user User to suspend
   */
  public void suspend(User user) {
    user.clearVerificationToken();
    user.setVerified(false);
    update(user);
  }

  /**
   * Sets account as verified without setting a password.
   *
   * @param user
   */
  public void unsuspend(User user) {
    user.setVerified(true);
    update(user);
  }

  private String getRootPath() {
    try {
      if (!session.nodeExists(USER_PATH)) {
        session.getRootNode().addNode(USER_PATH.substring(1));
        session.save();
      }
      return USER_PATH;
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean checkPassword(User user, String candidatePassword) {
    // Unverified users may not log in
    if (!user.isVerified())
      return false;
    // Otherwise, check the credentials
    CryptedSimpleCredentials creds;
    try {
      creds = (CryptedSimpleCredentials)
          jackrabbitUser(user).getCredentials();
      SimpleCredentials candidate = new SimpleCredentials(
          creds.getUserID(), candidatePassword.toCharArray());
      return creds.matches(candidate);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public void setPassword(User user, String clearPassword)
      throws RepositoryException {
    user.setVerified(true);
    update(user);
    jackrabbitUser(user).changePassword(clearPassword);
  }

  public org.apache.jackrabbit.api.security.user.User jackrabbitUser(User user)
      throws RepositoryException {
    return
        (org.apache.jackrabbit.api.security.user.User)
        session.getUserManager().getAuthorizable(jackrabbitAuthUserId(user));
  }

  private String jackrabbitAuthUserId(User user) {
    return user.getJackrabbitUserId();
  }

  // This is relying on knowing how getJackrabbitUserId() works, which is
  // probably something that should be fixed.
  public User findByJackrabbitID(String userId) {
    // if it's admin don't log the exception
    if("admin".equals(userId)) {
      return null;
    }
    try {
      return loadById(userId);
    } catch (JcrMappingException e) {
      Logger.debug("Unable to find user by id: "+userId, e);
      return null;
    }
  }

}
