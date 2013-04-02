package service.filestore;

import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;

import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.roles.Admin;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

public class FileStore {

  public static final String FILE_STORE_PATH = "filestore";

  @Inject
  public FileStore(final JcrSessionFactory sessionFactory) {
    sessionFactory.inSession(new Function<Session, Node>() {
      @Override
      public Node apply(Session session) {
        try {
          Node root;
          if (session.getRootNode().hasNode(FILE_STORE_PATH)) {
            root = session.getNode("/"+FILE_STORE_PATH);
          } else {
            root = session.getRootNode().addNode(FILE_STORE_PATH);
            (new Folder(root)).resetPermissions();
          }
          return root;
        } catch (PathNotFoundException e) {
          throw new RuntimeException(e);
        } catch (RepositoryException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  public Manager getManager(Session session) {
    return new Manager(session);
  }

  public static class Manager {

    private final Session session;

    protected Manager(Session session) {
      this.session = session;
    }

    public FileStore.Folder getFolder(String relPath)
        throws RepositoryException {
      try {
        if (relPath == "")
          return getRoot();
        return new Folder(getRootNode().getNode(relPath));
      } catch (PathNotFoundException e) {
        return null;
      }
    }

    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      try {
        return ImmutableSet.<FileStore.Folder>of(getRoot());
      } catch (AccessControlException e) {
        // TODO: Handle access for users without read on the root node
      }
      return Collections.emptySet();
    }

    protected Folder getRoot() throws RepositoryException {
      return new Folder(getRootNode());
    }

    protected Node getRootNode() throws RepositoryException {
      return session.getRootNode().getNode(FILE_STORE_PATH);
    }

  }

  public static class Folder extends NodeWrapper {

    protected Folder(final Node node) {
      super(node);
    }

    public Folder createFolder(String name) throws ItemExistsException,
        PathNotFoundException, NoSuchNodeTypeException, LockException,
        VersionException, ConstraintViolationException, RepositoryException {
      return new Folder(node.addNode(name, NodeType.NT_FOLDER));
    }

    public File createFile(String name, String mime, InputStream data)
        throws RepositoryException {
      return new File(node, name, mime, data);
    }

    public File getFile(String name) throws RepositoryException {
      try {
        return new File(node.getNode(name));
      } catch (PathNotFoundException e) {
        return null;
      }
    }

    public Set<File> getFiles() throws RepositoryException {
      ImmutableSet.Builder<File> set = ImmutableSet.<File>builder();
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (child.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)) {
          set.add(new File(child));
        }
      }
      return set.build();
    }

    public void resetPermissions() throws RepositoryException {
      Group group = Admin.getInstance(session()).getGroup();
      AccessControlManager acm = session().getAccessControlManager();
      AccessControlList acl = AccessControlUtils.getAccessControlList(
          acm, node.getPath());
      Principal everyone = EveryonePrincipal.getInstance();
      for (AccessControlEntry entry : acl.getAccessControlEntries()) {
        if (entry.getPrincipal().equals(everyone)) {
          acl.removeAccessControlEntry(entry);
        }
      }
      acl.addAccessControlEntry(group.getPrincipal(),
          AccessControlUtils.privilegesFromNames(session(),
              Privilege.JCR_READ,
              Privilege.JCR_ADD_CHILD_NODES,
              Privilege.JCR_REMOVE_CHILD_NODES,
              Privilege.JCR_MODIFY_PROPERTIES));
      acm.setPolicy(node.getPath(), acl);
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof Folder) {
        return ((Folder) other).equals(node);
      } else if (other instanceof Node) {
        return node.equals(other);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return node.hashCode();
    }

  }

  public static class File extends NodeWrapper {

    protected File(final Node node) {
      super(node);
    }

    protected File(final Node parent, final String name, final String mime,
        final InputStream data) throws RepositoryException {
      super(JcrUtils.putFile(parent, name, mime, data));
    }

    public File update(final String mime, InputStream data)
        throws AccessDeniedException, ItemNotFoundException,
        RepositoryException {
      return new File(
          JcrUtils.putFile(node.getParent(), node.getName(), mime, data));
    }

  }

  protected abstract static class NodeWrapper {

    protected final Node node;

    protected NodeWrapper(final Node node) {
      this.node = node;
    }

    protected Session session() throws RepositoryException {
      return node.getSession();
    }

    public String getName() throws RepositoryException {
      return node.getName();
    }

    public String getPath() throws RepositoryException {
      return node.getPath().substring(FILE_STORE_PATH.length()+1);
    }

  }



}
