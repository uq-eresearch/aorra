package service;

import com.google.common.collect.ImmutableSet;
import com.wingnest.play2.jackrabbit.Jcr;
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import play.Application;
import play.Plugin;

public class FreeformFileStore extends Plugin
    implements ContributionFolderProvider
{

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

  @Override
  public Set<Node> getAll(Session session) {
    try {
      @SuppressWarnings("unchecked")
      Iterator<Node> nodeIterator = (Iterator<Node>)
        session.getRootNode().getNode(FILE_STORE_PATH).getNodes();
      return ImmutableSet.copyOf(nodeIterator);
    } catch (PathNotFoundException e) {
      throw new RuntimeException(e);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<Node> getWritable(Session session) {
    final String HYPOTHETICAL_NEW_CHILD_NODE = "test-file.which.must-not-exist";
    ImmutableSet.Builder<Node> builder = ImmutableSet.<Node>builder();
    try {
      for (Node n : getAll(session)) {
        final String newPath = n.getPath()+"/"+HYPOTHETICAL_NEW_CHILD_NODE;
        // The ADD_NODE privilege checks the ability to create new node at a
        // particular absolute path. It will fail for existing folder.
        if (session.hasPermission(newPath, Session.ACTION_ADD_NODE)) {
          builder.add(n);
        }
      }
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return builder.build();
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
        AccessControlManager acm = session.getAccessControlManager();
        AccessControlList acl = AccessControlUtils.getAccessControlList(
            acm, this.node.getPath());
        Principal everyone = EveryonePrincipal.getInstance();
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
          if (entry.getPrincipal().equals(everyone)) {
            acl.removeAccessControlEntry(entry);
          }
        }
        acl.addAccessControlEntry(group.getPrincipal(),
            AccessControlUtils.privilegesFromNames(session,
                Privilege.JCR_READ,
                Privilege.JCR_ADD_CHILD_NODES,
                Privilege.JCR_REMOVE_CHILD_NODES,
                Privilege.JCR_MODIFY_PROPERTIES));
        acm.setPolicy(this.node.getPath(), acl);
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
