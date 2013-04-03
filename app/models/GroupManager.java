package models;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

import com.google.common.collect.ImmutableSet;

public class GroupManager {

  public static final String FLAG_NAME = "managedGroup";

  public final JackrabbitSession session;

  public GroupManager(Session session) {
    this.session = (JackrabbitSession) session;
  }

  public Group create(String groupName) throws AuthorizableExistsException {
    Group group;
    try {
      group = session.getUserManager().createGroup(groupName);
      group.setProperty(FLAG_NAME, session.getValueFactory().createValue(""));
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    } catch (AuthorizableExistsException e) {
      throw e;
    } catch (UnsupportedRepositoryOperationException e) {
      throw new RuntimeException(e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return group;
  }

  public Set<Group> list() {
    ImmutableSet.Builder<Group> set = ImmutableSet.<Group>builder();
    try {
      for (Iterator<Authorizable> iter = fetchGroups(); iter.hasNext();) {
        Authorizable authorizable = iter.next();
        if (authorizable instanceof Group) {
          set.add((Group) authorizable);
        }
      }
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedRepositoryOperationException e) {
      throw new RuntimeException(e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return set.build();
  }

  public Group find(String groupName) throws PathNotFoundException {
    Group group;
    try {
      Authorizable a = session.getUserManager().getAuthorizable(groupName);
      if (a == null)
        throw new PathNotFoundException("Group does not exist.");
      if (!(a instanceof Group)) {
        throw new PathNotFoundException(groupName+" is not a group!");
      }
      if (a.getProperty(FLAG_NAME) == null) {
        throw new PathNotFoundException(groupName+" is not a manged group.");
      }
      group = (Group) a;
    } catch (AccessDeniedException e) {
      throw new RuntimeException(e);
    } catch (UnsupportedRepositoryOperationException e) {
      throw new RuntimeException(e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return group;
  }

  public void delete(String groupName) throws PathNotFoundException {
    Group group = find(groupName);
    try {
      group.remove();
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  protected Iterator<Authorizable> fetchGroups() throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    return session.getUserManager().findAuthorizables(
        FLAG_NAME, "", UserManager.SEARCH_TYPE_GROUP);
  }


}
