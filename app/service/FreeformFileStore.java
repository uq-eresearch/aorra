package service;

import com.google.common.collect.ImmutableSet;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;
import com.wingnest.play2.jackrabbit.Jcr;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import play.Application;
import play.Plugin;
import org.apache.jackrabbit.api.security.user.Group;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

import org.apache.jackrabbit.api.security.user.UserManager;
import javax.jcr.Node;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;

public class FreeformFileStore extends Plugin {

  public static final String FILE_STORE_PATH = "filestore";
  public static final String GROUP_PATH = "contributionGroups";

  private Application application;

  public FreeformFileStore(Application application) {
    this.application = application;
  }

  public void onStart() {
    try {
      initFileStore();
    } catch (RepositoryException re) {
      throw new RuntimeException(re);
    }
  }

  @SuppressWarnings("unchecked")
  public Set<Node> getContributionFolders(Session session) {
    try {
      return ImmutableSet.copyOf((Iterator<Node>)
          session.getRootNode().getNode(FILE_STORE_PATH).getNodes());
    } catch (PathNotFoundException e) {
      throw new RuntimeException(e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  private Session getAdminSession() throws RepositoryException {
    return Jcr.login(
        application.configuration().getString(ConfigConsts.CONF_JCR_USERID),
        application.configuration().getString(ConfigConsts.CONF_JCR_PASSWORD));
  }

  protected void initFileStore() throws RepositoryException {
    Session session = getAdminSession();
    try {
      GroupManager gm = new GroupManager(session);
      ContributionGroup rs = gm.group("Reef Secretariat");
      rs.belongsTo(gm.group("Catchment Loads"));
      rs.belongsTo(gm.group("Groundcover"));
      rs.belongsTo(gm.group("Management Practices"));
      rs.belongsTo(gm.group("Marine"));
      session.save();
    } finally {
      session.logout();
    }
  }

  private class GroupManager {

    private final Session session;
    private final UserManager userManager;

    public GroupManager(final Session session)
        throws UnsupportedRepositoryOperationException, RepositoryException {
      this.session = session;
      this.userManager = ((JackrabbitSession) session).getUserManager();
    }

    public ContributionGroup group(String name) throws RepositoryException {
      final Authorizable auth = userManager.getAuthorizable(name);
      final Group g;
      if (auth == null) {
        g = userManager.createGroup(new NamedPrincipal(name), GROUP_PATH);
      } else if (auth instanceof Group) {
        g = (Group) auth;
      } else {
        throw new RuntimeException("Authorizable already exists! Not a group: "
            + auth);
      }
      // Create the group folder
      FolderHelper folder = rootFolder().findOrCreateFolder(name);
      // Set appropriate ACLs
      folder.setOwner(session, g);
      return new ContributionGroup(g);
    }

    private FolderHelper rootFolder() throws RepositoryException {
      return (new FolderHelper(session.getRootNode()))
          .findOrCreateFolder(FILE_STORE_PATH);
    }

    private class FolderHelper {

      public final Node node;

      public FolderHelper(Node node) {
        this.node = node;
      }

      public FolderHelper findOrCreateFolder(String path) throws RepositoryException {
        try {
          return new FolderHelper(this.node.getNode(path));
        } catch (PathNotFoundException e) {
          try {
            return new FolderHelper(
                this.node.addNode(path, NodeType.NT_FOLDER));
          } catch (ItemExistsException e1) {
            // Recurse to find newly-created node
            return findOrCreateFolder(path);
          } catch (PathNotFoundException e1) {
            throw new RuntimeException(e1);
          } catch (NoSuchNodeTypeException e1) {
            throw new RuntimeException(e1);
          } catch (LockException e1) {
            throw new RuntimeException(e1);
          } catch (VersionException e1) {
            throw new RuntimeException(e1);
          } catch (ConstraintViolationException e1) {
            throw new RuntimeException(e1);
          }
        }
      }

      public void setOwner(Session session, Group group)
          throws RepositoryException {
        AccessControlUtils.denyAllToEveryone(session, this.node.getPath());
        AccessControlUtils.addAccessControlEntry(session, this.node.getPath(),
            group.getPrincipal(), AccessControlUtils.privilegesFromNames(
                session, Privilege.JCR_READ, Privilege.JCR_WRITE), true);
      }

    }

  }

  private class NamedPrincipal implements Principal {

    private final String name;

    public NamedPrincipal(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

  }

  private class ContributionGroup {

    public final Group group;

    public ContributionGroup(Group group) {
      this.group = group;
    }

    public void belongsTo(ContributionGroup other) throws RepositoryException {
      if (!other.group.isMember(group)) {
        other.group.addMember(group);
      }
    }
  }

}