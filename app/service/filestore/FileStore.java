package service.filestore;

import java.io.InputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import models.GroupManager;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;

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

public class FileStore {

  public enum Permission {
    NONE, RO, RW;

    public boolean isAtLeast(Permission other) {
      return this.compareTo(other) >= 0;
    }

  }

  public static final String FILE_STORE_PATH = "/filestore";
  public static final String FILE_STORE_NODE_NAME =
      StringUtils.stripStart(FILE_STORE_PATH, "/");

  private final ActorRef eventManager;

  @Inject
  public FileStore(final JcrSessionFactory sessionFactory) {
    eventManager = Akka.system().actorOf(new Props(EventManager.class));
    sessionFactory.inSession(new Function<Session, Node>() {
      @Override
      public Node apply(Session session) {
        try {
          Node root;
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
    Node node;
    try {
      node = session.getNode(path);
      Logger.debug("Actual:");
      {
        AccessControlManager acm = session.getAccessControlManager();
        AccessControlList acl = AccessControlUtils.getAccessControlList(
            acm, node.getPath());
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
          for (Privilege p : entry.getPrivileges()) {
            Logger.debug(entry.getPrincipal().getName()+" "+p.getName());
          }
        }
      }
      Logger.debug("Effective:");
      AccessControlPolicy[] policies = session.getAccessControlManager()
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

  public Manager getManager(Session session) {
    return new Manager(session, eventManager);
  }

  public ActorRef getEventManager() {
    return eventManager;
  }

  protected static FileOrFolder fromNode(Node node,
      ActorRef eventManager) throws RepositoryException {
    if (node.getPrimaryNodeType().isNodeType(NodeType.NT_FILE))
      return new File(node, eventManager);
    else
      return new Folder(node, eventManager);
  }

  public static class Manager {

    private final Session session;
    private final ActorRef eventManager;

    protected Manager(Session session, ActorRef eventManager) {
      this.session = session;
      this.eventManager = eventManager;
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
            return fromNode(node, eventManager);
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
      return new Folder(getRootNode(), eventManager);
    }

    protected Node getRootNode() throws RepositoryException {
      return session.getRootNode().getNode(FILE_STORE_NODE_NAME);
    }

  }

  public static class Folder extends NodeWrapper {

    protected Folder(final Node node, final ActorRef eventManager) {
      super(node, eventManager);
    }

    public Folder createFolder(String name) throws ItemExistsException,
        PathNotFoundException, NoSuchNodeTypeException, LockException,
        VersionException, ConstraintViolationException, RepositoryException {
      if(getFileOrFolder(name) != null) {
          throw new RuntimeException(String.format("file or folder already exists '%s'", name));
      } else {
        Folder folder = new Folder(
            node.addNode(name, NodeType.NT_FOLDER), eventManager);
        eventManager.tell(FileStoreEvent.create(folder), null);
        return folder;
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
          File file = new File(node, name, mime, data, eventManager);
          eventManager.tell(FileStoreEvent.create(file), null);
          return file;
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
          set.add(new Folder(child, eventManager));
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
                return fromNode(child, eventManager);
            }
        }
        return null;
    }

    public Set<File> getFiles() throws RepositoryException {
      ImmutableSet.Builder<File> set = ImmutableSet.<File>builder();
      for (final Node child : JcrUtils.getChildNodes(node)) {
        if (child.getPrimaryNodeType().isNodeType(NodeType.NT_FILE)) {
          set.add(new File(child, eventManager));
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
              Privilege.JCR_MODIFY_PROPERTIES,
              Privilege.JCR_NODE_TYPE_MANAGEMENT));
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

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      FileStoreEvent event = FileStoreEvent.delete(this);
      super.delete();
      eventManager.tell(event, null);
    }

  }

  public static class File extends NodeWrapper {

    protected File(final Node node, final ActorRef eventManager) {
      super(node, eventManager);
    }

    protected File(final Node parent, final String name, final String mime,
        final InputStream data, final ActorRef eventManager)
            throws RepositoryException {
      super(JcrUtils.putFile(parent, name, mime, data), eventManager);
    }

    public File update(final String mime, InputStream data)
        throws AccessDeniedException, ItemNotFoundException,
        RepositoryException {
      File f = new File(
          JcrUtils.putFile(node.getParent(), node.getName(), mime, data),
          eventManager);
      eventManager.tell(FileStoreEvent.update(this), null);
      return f;
    }

    public InputStream getData() {
      try {
        return JcrUtils.readFile(node);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public String getMimeType()
        throws ValueFormatException, PathNotFoundException, RepositoryException {
      return node.getNode(Node.JCR_CONTENT)
          .getProperty("jcr:mimeType").getString();
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      FileStoreEvent event = FileStoreEvent.delete(this);
      super.delete();
      eventManager.tell(event, null);
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
    protected final ActorRef eventManager;

    protected NodeWrapper(final Node node, final ActorRef eventManager) {
      this.node = node;
      this.eventManager = eventManager;
    }

    protected Session session() throws RepositoryException {
      return node.getSession();
    }

    @Override
    public int getDepth() throws RepositoryException {
      if (node.getPath().equals(FILE_STORE_PATH)) {
        return 0;
      } else {
        return (new Folder(node.getParent(), eventManager)).getDepth() + 1;
      }
    }

    public Folder getParent() throws RepositoryException {
      return new Folder(node.getParent(), eventManager);
    }

    public Map<Group, Permission> getGroupPermissions() throws RepositoryException {
      final ImmutableMap.Builder<Group, Permission> b =
          ImmutableMap.<Group, Permission>builder();
      final Set<Group> groups = (new GroupManager(node.getSession())).list();
      final Map<Principal, Permission> perms = getPrincipalPermissions();
      for (Group group : groups) {
        b.put(group, resolvePermission(group, perms));
      }
      return b.build();
    }

    private Permission resolvePermission(Group group, Map<Principal, Permission> perms) throws RepositoryException {
      Principal p = group.getPrincipal();
      if (perms.containsKey(p)) {
        return perms.get(p);
      } else {
        SortedSet<Permission> inherited = new TreeSet<Permission>();
        // Get all potentially inherited permissions
        Iterator<Group> iter = group.declaredMemberOf();
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

    private Map<Principal, Permission> getPrincipalPermissions() throws RepositoryException {
      final ImmutableMap.Builder<Principal, Permission> b =
          ImmutableMap.<Principal, Permission>builder();
      JackrabbitSession session = (JackrabbitSession) node.getSession();
      AccessControlPolicy[] policies = session.getAccessControlManager()
          .getEffectivePolicies(node.getPath());
      for (AccessControlPolicy policy : policies) {
        AccessControlList acl = (AccessControlList) policy;
        for (AccessControlEntry entry : acl.getAccessControlEntries()) {
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
