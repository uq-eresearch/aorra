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

import org.apache.commons.lang.StringUtils;
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

  public static final String FILE_STORE_PATH = "/filestore";
  public static final String FILE_STORE_NODE_NAME =
      StringUtils.stripStart(FILE_STORE_PATH, "/");

  @Inject
  public FileStore(final JcrSessionFactory sessionFactory) {
    sessionFactory.inSession(new Function<Session, Node>() {
      @Override
      public Node apply(Session session) {
        try {
          Node root;
          if (session.getRootNode().hasNode(FILE_STORE_NODE_NAME)) {
            root = session.getNode(FILE_STORE_PATH);
          } else {
            root = session.getRootNode().addNode(FILE_STORE_NODE_NAME);
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

  private static FileOrFolder fromNode(Node node) throws RepositoryException {
    if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
      return new File(node);
    else
      return new Folder(node);
  }

  public static class Manager {

    private final Session session;

    protected Manager(Session session) {
      this.session = session;
    }

    public FileStore.Folder getFolder(String absPath)
      throws RepositoryException {
      FileOrFolder f = getFileOrFolder(absPath);
      if (f != null && f.isFolder()) {
        return (Folder)f;
      } else {
        return null;
      }
    }

    public FileOrFolder getFileOrFolder(String absPath) throws RepositoryException {
      if (absPath.equals("/")) {
        return getRoot();
      } else {
        try {
          Node node = getRootNode().getNode(
              StringUtils.stripStart(absPath, "/"));
          if (node != null) {
            return fromNode(node);
          }
        } catch (PathNotFoundException e) {}
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

    public Folder getRoot() throws RepositoryException {
      return new Folder(getRootNode());
    }

    protected Node getRootNode() throws RepositoryException {
      return session.getRootNode().getNode(FILE_STORE_NODE_NAME);
    }

  }

  public static class Folder extends NodeWrapper {

    protected Folder(final Node node) {
      super(node);
    }

    public Folder createFolder(String name) throws ItemExistsException,
        PathNotFoundException, NoSuchNodeTypeException, LockException,
        VersionException, ConstraintViolationException, RepositoryException {
      if(getFileOrFolder(name) != null) {
          throw new RuntimeException(String.format("file or folder already exists '%s'", name));
      } else {
          return new Folder(node.addNode(name, NodeType.NT_FOLDER));
      }
    }

    public File createFile(String name, String mime, InputStream data)
        throws RepositoryException {
      return createOrOverwriteFile(name, mime, data);
    }

    public File createOrOverwriteFile(String name, String mime, InputStream data)
        throws RepositoryException {
        FileOrFolder f = getFileOrFolder(name);
        if(f!=null && f.isFolder()) {
            throw new RuntimeException(String.format("Can't create file '%s'." +
                    " Folder with same name already exists", name));
        } else {
            return new File(node, name, mime, data);
        }
    }

    public File getFile(String name) throws RepositoryException {
        FileOrFolder f = getFileOrFolder(name);
        if((f!=null) && (!f.isFolder())) {
            return (File)f;
        } else {
            return null;
        }
    }

    public Set<Folder> getFolders() throws RepositoryException {
      ImmutableSet.Builder<Folder> set = ImmutableSet.<Folder>builder();
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (child.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER)) {
          set.add(new Folder(child));
        }
      }
      return set.build();
    }

    public Folder getFolder(String name) throws RepositoryException {
        for(Folder folder : getFolders()) {
            if(StringUtils.equals(name, folder.getName())) {
                return folder;
            }
        }
        return null;
    }

    public FileOrFolder getFileOrFolder(String name) throws RepositoryException {
        for (final Node child : JcrUtils.getChildNodes(node)) {
            if(StringUtils.equals(child.getName(), name)) {
                return fromNode(child);
            }
        }
        return null;
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

    @Override
    public boolean isFolder() {
        return true;
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

    @Override
    public boolean isFolder() {
        return false;
    }

  }

  public static interface FileOrFolder {
      public int getDepth() throws RepositoryException;
      public String getName() throws RepositoryException;
      public String getPath() throws RepositoryException;
      public void delete() throws AccessDeniedException, VersionException,
          LockException, ConstraintViolationException, RepositoryException;
      public boolean isFolder();
  }

  protected abstract static class NodeWrapper implements FileOrFolder {

    protected final Node node;

    protected NodeWrapper(final Node node) {
      this.node = node;
    }

    protected Session session() throws RepositoryException {
      return node.getSession();
    }

    @Override
    public int getDepth() throws RepositoryException {
      if (node.getPath().equals(FILE_STORE_PATH)) {
        return 0;
      } else {
        return (new Folder(node.getParent())).getDepth() + 1;
      }
    }

    public String getIdentifier() throws RepositoryException {
      return node.getIdentifier();
    }

    @Override
    public String getName() throws RepositoryException {
      return node.getName();
    }

    @Override
    public String getPath() throws RepositoryException {
      final String absPath = node.getPath();
      if (absPath.equals(FILE_STORE_PATH))
        return "/";
      return absPath.substring(FILE_STORE_PATH.length());
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      node.remove();
    }

  }

}
