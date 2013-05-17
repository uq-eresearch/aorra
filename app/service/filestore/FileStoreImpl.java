package service.filestore;

import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import models.GroupManager;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.jcrom.Jcrom;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.EventManager.FileStoreEvent;
import service.filestore.roles.Admin;

import akka.actor.ActorRef;
import akka.actor.Props;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

public class FileStoreImpl implements FileStore {

  public static final String FILE_STORE_PATH = "/filestore";
  public static final String FILE_STORE_NODE_NAME =
      StringUtils.stripStart(FILE_STORE_PATH, "/");

  private final ActorRef eventManager;
  private final Jcrom jcrom;

  @Inject
  public FileStoreImpl(
      final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.jcrom = jcrom;
    eventManager = Akka.system().actorOf(new Props(EventManager.class));
    sessionFactory.inSession(new Function<Session, Node>() {
      @Override
      public Node apply(Session session) {
        try {
          final Node root;
          if (session.getRootNode().hasNode(FILE_STORE_NODE_NAME)) {
            root = session.getNode(FILE_STORE_PATH);
          } else {
            root = session.getRootNode().addNode(FILE_STORE_NODE_NAME);
            (new Folder(root, eventManager)).resetPermissions();
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

  protected static void debugPermissions(Session session, String path) {
    final Node node;
    try {
      node = session.getNode(path);
      Logger.debug("Actual:");
      {
        final AccessControlManager acm = session.getAccessControlManager();
        final AccessControlList acl = AccessControlUtils.getAccessControlList(
            acm, node.getPath());
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
          for (Privilege p : entry.getPrivileges()) {
            Logger.debug(entry.getPrincipal().getName()+" "+p.getName());
          }
        }
      }
      Logger.debug("Effective:");
      final AccessControlPolicy[] policies = session.getAccessControlManager()
          .getEffectivePolicies(node.getPath());
      for (AccessControlPolicy policy : policies) {
        AccessControlList acl = (AccessControlList) policy;
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
          for (Privilege p : entry.getPrivileges()) {
            Logger.debug(entry.getPrincipal().getName()+" "+p.getName());
          }
        }
      }
    } catch (PathNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /* (non-Javadoc)
   * @see service.filestore.FileStore#getManager(javax.jcr.Session)
   */
  @Override
  public Manager getManager(final Session session) {
    return new Manager(session, jcrom, eventManager);
  }

  /* (non-Javadoc)
   * @see service.filestore.FileStore#getEventManager()
   */
  @Override
  public ActorRef getEventManager() {
    return eventManager;
  }

  protected static FileOrFolder fromNode(final Node node,
      final ActorRef eventManager) throws RepositoryException {
    if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
      return new File(node, eventManager);
    else
      return new Folder(node, eventManager);
  }

  public static class Manager implements FileStore.Manager {

    private final Session session;
    private final Jcrom jcrom;
    private final ActorRef eventManager;

    protected Manager(final Session session, final Jcrom jcrom, final ActorRef eventManager) {
      this.session = session;
      this.jcrom = jcrom;
      this.eventManager = eventManager;
    }

    @Override
    public FileOrFolder getFileOrFolder(final String absPath)
        throws RepositoryException {
      if (absPath.equals("/")) {
        return getRoot();
      } else {
        try {
          Node node = getRootNode().getNode(
              StringUtils.stripStart(absPath, "/"));
          if (node != null) {
            return fromNode(node, eventManager);
          }
        } catch (PathNotFoundException e) {
        }
        return null;
      }
    }

    @Override
    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      try {
        session.checkPermission(FILE_STORE_PATH, "read");
        return ImmutableSet.<FileStore.Folder>of(getRoot());
      } catch (AccessControlException e) {
        // TODO: Handle access for users without read on the root node
      }
      return Collections.emptySet();
    }

    @Override
    public FileStore.Folder getRoot() throws RepositoryException {
      return new Folder(getRootNode(), eventManager);
    }

    protected Node getRootNode() throws RepositoryException {
      return session.getRootNode().getNode(FILE_STORE_NODE_NAME);
    }

  }

  public static class Folder extends NodeWrapper implements FileStore.Folder {

    protected Folder(final Node node, final ActorRef eventManager) throws RepositoryException {
      super(node, eventManager);
    }

    @Override
    public Folder createFolder(final String name) throws RepositoryException {
      if (getFileOrFolder(name) != null) {
        throw new RuntimeException(String.format(
            "file or folder already exists '%s'", name));
      } else {
        Folder folder = new Folder(node.addNode(name, NodeType.NT_FOLDER),
            eventManager);
        eventManager.tell(FileStoreEvent.create(folder), null);
        return folder;
      }
    }

    @Override
    public File createFile(final String name, final String mime,
        final InputStream data) throws RepositoryException {
      return createOrOverwriteFile(name, mime, data);
    }

    private File createOrOverwriteFile(final String name, final String mime,
        final InputStream data) throws RepositoryException {
      final FileOrFolder f = getFileOrFolder(name);
      if (f != null && f instanceof Folder) {
        throw new RuntimeException(String.format("Can't create file '%s'."
            + " Folder with same name already exists", name));
      } else {
        final File file = new File(node, name, mime, data, eventManager);
        eventManager.tell(FileStoreEvent.create(file), null);
        return file;
      }
    }

    @Override
    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      final ImmutableSet.Builder<FileStore.Folder> set =
          ImmutableSet.<FileStore.Folder>builder();
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (child.getPrimaryNodeType().isNodeType(NodeType.NT_FOLDER)) {
          set.add(new Folder(child, eventManager));
        }
      }
      return set.build();
    }

    public FileStore.Folder getFolder(final String name)
        throws RepositoryException {
      for (FileStore.Folder folder : getFolders()) {
        if (StringUtils.equals(name, folder.getName())) {
          return folder;
        }
      }
      return null;
    }

    @Override
    public FileOrFolder getFileOrFolder(final String name)
        throws RepositoryException {
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (StringUtils.equals(child.getName(), name)) {
          return fromNode(child, eventManager);
        }
      }
      return null;
    }

    @Override
    public Set<FileStore.File> getFiles() throws RepositoryException {
      final ImmutableSet.Builder<FileStore.File> set =
          ImmutableSet.<FileStore.File> builder();
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (child.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)) {
          set.add(new File(child, eventManager));
        }
      }
      return set.build();
    }

    public void resetPermissions() throws RepositoryException {
      final Group group = Admin.getInstance(session()).getGroup();
      final AccessControlManager acm = session().getAccessControlManager();
      final JackrabbitAccessControlList acl = AccessControlUtils
          .getAccessControlList(acm, node.getPath());
      final Principal everyone = EveryonePrincipal.getInstance();
      for (AccessControlEntry entry : acl.getAccessControlEntries()) {
        if (entry.getPrincipal().equals(everyone)) {
          acl.removeAccessControlEntry(entry);
        }
      }
      if (node.getPath().equals(FILE_STORE_PATH)) {
        // Deny everyone everything by default on root (which should propagate)
        acl.addEntry(EveryonePrincipal.getInstance(), AccessControlUtils
            .privilegesFromNames(session(), Privilege.JCR_ALL), false);
      }
      // Explicitly allow read permissions to admin group
      acl.addEntry(group.getPrincipal(), AccessControlUtils
          .privilegesFromNames(session(), Privilege.JCR_READ,
              Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_REMOVE_CHILD_NODES,
              Privilege.JCR_MODIFY_PROPERTIES,
              Privilege.JCR_NODE_TYPE_MANAGEMENT, Privilege.JCR_REMOVE_NODE,
              Privilege.JCR_REMOVE_CHILD_NODES), true);
      acm.setPolicy(node.getPath(), acl);
    }

    @Override
    public boolean equals(final Object other) {
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
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final FileStoreEvent event = FileStoreEvent.delete(this);
      super.delete();
      eventManager.tell(event, null);
    }

  }

  public static class File extends NodeWrapper implements FileStore.File {

    protected File(final Node node, final ActorRef eventManager)
        throws RepositoryException {
      super(node, eventManager);
    }

    protected File(final Node parent, final String name, final String mime,
        final InputStream data, final ActorRef eventManager)
            throws RepositoryException {
      super(JcrUtils.putFile(parent, name, mime, data), eventManager);
    }

    @Override
    public File update(final String mime, final InputStream data)
        throws RepositoryException {
      File f = new File(JcrUtils.putFile(node.getParent(), node.getName(),
          mime, data), eventManager);
      eventManager.tell(FileStoreEvent.update(this), null);
      return f;
    }

    @Override
    public InputStream getData() {
      try {
        return JcrUtils.readFile(node);
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getMimeType() {
      try {
        return node.getNode(Node.JCR_CONTENT)
            .getProperty("jcr:mimeType").getString();
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final FileStoreEvent event = FileStoreEvent.delete(this);
      super.delete();
      eventManager.tell(event, null);
    }

  }

  protected abstract static class NodeWrapper implements FileOrFolder {

    protected final Node node;
    protected final ActorRef eventManager;
    private final String id;
    private final String name;
    private final int depth;
    private final String path;

    protected NodeWrapper(final Node node, final ActorRef eventManager)
      throws RepositoryException
    {
      this.node = node;
      this.eventManager = eventManager;
      this.id = node.getIdentifier();
      this.name = node.getName();
      this.path = calculatePath();
      this.depth = calculateDepth();
    }

    protected Session session() throws RepositoryException {
      return node.getSession();
    }

    @Override
    public int getDepth() {
      return depth;
    }

    public int calculateDepth() throws RepositoryException {
      if (node.getPath().equals(FILE_STORE_PATH)) {
        return 0;
      } else {
        return (new Folder(node.getParent(), eventManager)).getDepth() + 1;
      }
    }

    public Folder getParent() throws RepositoryException {
      return new Folder(node.getParent(), eventManager);
    }

    public Map<String, Permission> getGroupPermissions()
        throws RepositoryException {
      final ImmutableMap.Builder<String, Permission> b = ImmutableMap
          .<String, Permission> builder();
      final Set<Group> groups = (new GroupManager(node.getSession())).list();
      final Map<Principal, Permission> perms = getPrincipalPermissions();
      for (final Group group : groups) {
        b.put(group.getPrincipal().getName(), resolvePermission(group, perms));
      }
      return b.build();
    }

    private Permission resolvePermission(final Group group,
        final Map<Principal, Permission> perms) throws RepositoryException {
      final Principal p = group.getPrincipal();
      if (perms.containsKey(p)) {
        return perms.get(p);
      } else {
        final SortedSet<Permission> inherited = new TreeSet<Permission>();
        // Get all potentially inherited permissions
        final Iterator<Group> iter = group.declaredMemberOf();
        while (iter.hasNext()) {
          inherited.add(resolvePermission(iter.next(), perms));
        }
        if (inherited.isEmpty()) {
          return Permission.NONE;
        }
        // Use natural order of enum to return highest permission
        return inherited.last();
      }
    }

    private Map<Principal, Permission> getPrincipalPermissions()
        throws RepositoryException {
      final ImmutableMap.Builder<Principal, Permission> b = ImmutableMap
          .<Principal, Permission> builder();
      final JackrabbitSession session = (JackrabbitSession) node.getSession();
      final AccessControlPolicy[] policies = session.getAccessControlManager()
          .getEffectivePolicies(node.getPath());
      for (AccessControlPolicy policy : policies) {
        final JackrabbitAccessControlList acl = (JackrabbitAccessControlList) policy;
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
          final JackrabbitAccessControlEntry jackrabbitEntry = (JackrabbitAccessControlEntry) entry;
          // Ignore deny entries (we shouldn't see any other than Everybody)
          if (!jackrabbitEntry.isAllow()) {
            continue;
          }
          final Set<String> privilegeNames = new HashSet<String>();
          for (Privilege privilege : entry.getPrivileges()) {
            // Add privilege
            privilegeNames.add(privilege.getName());
            // Add constituent privileges
            for (Privilege rp : privilege.getAggregatePrivileges()) {
              privilegeNames.add(rp.getName());
            }
          }
          if (privilegeNames.contains("jcr:addChildNodes")) {
            b.put(entry.getPrincipal(), Permission.RW);
          } else if (privilegeNames.contains("jcr:read")) {
            b.put(entry.getPrincipal(), Permission.RO);
          } else {
            b.put(entry.getPrincipal(), Permission.NONE);
          }
        }
      }
      return b.build();
    }


    @Override
    public String getIdentifier() {
      return id;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getPath() {
      return path;
    }

    public String calculatePath() throws RepositoryException {
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
