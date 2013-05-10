package service.filestore.roles;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

public class Admin {

  public static final String GROUP_ID = "filestoreAdmin";

  protected final Group group;

  protected Admin(final Group group) {
    this.group = group;
  }

  /**
   * Get the underlying group for the admin instance.
   *
   * @return Jackrabbit group
   */
  public Group getGroup() {
    return group;
  }

  /**
   * Get the admin group instance, creating it if necessary
   *
   * @param session
   *          the session to use to fetch this instance
   * @return admin instance bound to the provided session
   */
  public static Admin getInstance(final Session session)
      throws RepositoryException {
    return new Admin(getUnderlyingGroup((JackrabbitSession) session));
  }

  protected static Group getUnderlyingGroup(final JackrabbitSession session)
      throws RepositoryException {
    final UserManager um = session.getUserManager();
    try {
      // Create group
      return um.createGroup(GROUP_ID);
    } catch (AuthorizableExistsException e) {
      // Great! Find it instead
      return findAdminGroup(um);
    }
  }

  protected static Group findAdminGroup(final UserManager um)
      throws RepositoryException {
    return (Group) um.getAuthorizable(GROUP_ID);
  }

}