package service.filestore;

import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

import models.GroupManager;
import models.User;
import models.UserDAO;
import models.filestore.Child;
import models.filestore.FileDAO;
import models.filestore.FolderDAO;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;
import org.jcrom.util.PathUtils;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.EventManager.FileStoreEvent;
import service.filestore.roles.Admin;
import akka.actor.ActorRef;
import akka.actor.Props;
import jackrabbit.AorraAccessManager;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
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
    Logger.debug(this+" - Creating file store.");
    eventManager = Akka.system().actorOf(new Props(EventManager.class));
    sessionFactory.inSession(new Function<Session, Session>() {
      @Override
      public Session apply(Session session) {
        try {
          final FolderDAO dao = new FolderDAO(session, jcrom);
          if (dao.get(FILE_STORE_PATH) == null) {
            final models.filestore.Folder entity =
                new models.filestore.Folder(null, FILE_STORE_NODE_NAME);
            final String path =
                FILE_STORE_PATH.replaceFirst(FILE_STORE_NODE_NAME+"$", "");
            Logger.debug("Creating new filestore root at "+path);
            dao.create(path, entity);
            ((FileStoreImpl.Folder) getManager(session).getRoot())
              .resetPermissions();
          }
          return session;
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

  public static class Manager implements FileStore.Manager {

    private final Session session;
    private final Jcrom jcrom;
    private final ActorRef eventManager;
    private final FileDAO fileDAO;
    private final FolderDAO folderDAO;
    private final UserDAO userDAO;
    private final Map<String, User> userCache =
        new HashMap<String, User>();

    protected Manager(final Session session, final Jcrom jcrom, final ActorRef eventManager) {
      this.session = session;
      this.jcrom = jcrom;
      this.eventManager = eventManager;
      fileDAO = new FileDAO(session, jcrom);
      folderDAO = new FolderDAO(session, jcrom);
      userDAO = new UserDAO(session, jcrom);
    }

    @Override
    public FileOrFolder getByIdentifier(final String id)
        throws RepositoryException {
      try {
        return new Folder(getFolderDAO().loadById(id), this, eventManager);
      } catch (ClassCastException e) {
      } catch (NullPointerException e) {}
      try {
        return new File(getFileDAO().loadById(id), this, eventManager);
      } catch (ClassCastException e) {
      } catch (NullPointerException e) {}
      return null;
    }

    @Override
    public FileOrFolder getFileOrFolder(final String absPath)
        throws RepositoryException {
      if (absPath.equals("/"))
        return getRoot();
      final Deque<String> parts = new LinkedList<String>();
      for (String part : PathUtils.relativePath(absPath).split("/+")) {
        parts.add(part);
      }
      FileStore.Folder folder = getRoot();
      while (!parts.isEmpty()) {
        String part = parts.removeFirst();
        final FileOrFolder fof = folder.getFileOrFolder(part);
        if (fof == null) {
          Logger.debug("Unable to find file or folder at: "+absPath);
          return null;
        }
        if (parts.isEmpty())
          return fof;
        if (fof instanceof File) {
          Logger.debug("File "+part+" is not a folder in "+absPath);
          return null;
        }
        folder = (Folder) fof;
      }
      return null;
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
      return new FileStoreImpl.Folder(
          getFolderDAO().get(FILE_STORE_PATH), this, eventManager);
    }

    public Session getSession() {
      return session;
    }

    public Jcrom getJcrom() {
      return jcrom;
    }

    public FileDAO getFileDAO() {
      return fileDAO;
    }

    public FolderDAO getFolderDAO() {
      return folderDAO;
    }

    public User getUserFromJackrabbitID(final String jackrabbitUserId) {
      if (!userCache.containsKey(jackrabbitUserId)) {
        userCache.put(jackrabbitUserId,
            getUserDAO().findByJackrabbitID(jackrabbitUserId));
        Logger.debug("Adding "+jackrabbitUserId+" to user ID cache: "+
            userCache.get(jackrabbitUserId));
      }
      return userCache.get(jackrabbitUserId);
    }

    protected UserDAO getUserDAO() {
      return userDAO;
    }

  }

  public static class Folder extends NodeWrapper<models.filestore.Folder> implements FileStore.Folder {

    protected Folder(models.filestore.Folder entity, Manager filestoreManager,
        ActorRef eventManager) throws RepositoryException {
      super(entity, filestoreManager, eventManager);
    }

    @Override
    public Folder createFolder(final String name) throws RepositoryException {
      if (getFileOrFolder(name) != null) {
        throw new RuntimeException(String.format(
            "file or folder already exists '%s'", name));
      } else {
        final models.filestore.Folder newFolderEntity =
            filestoreManager.getFolderDAO().create(
                new models.filestore.Folder(entity, name));
        reload();
        final Folder folder = new Folder(
            newFolderEntity,
            filestoreManager,
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
      final FileOrFolder fof = getFileOrFolder(name);
      final File file;
      if (fof == null) {
        final models.filestore.File newFileEntity =
          filestoreManager.getFileDAO().create(
              new models.filestore.File(entity, name, mime, data));
        file = new File(newFileEntity, filestoreManager, eventManager);
        reload();
        Logger.debug("New file, version "+newFileEntity.getVersion());
        eventManager.tell(FileStoreEvent.create(file), null);
      } else if (fof instanceof Folder) {
        throw new RuntimeException(String.format("Can't create file '%s'."
            + " Folder with same name already exists", name));
      } else {
        file = ((File) fof);
        file.update(mime, data);
      }
      return file;
    }

    @Override
    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      final ImmutableSet.Builder<FileStore.Folder> set =
          ImmutableSet.<FileStore.Folder>builder();
      for (final Object child : entity.getFolders().values()) {
        set.add(new Folder((models.filestore.Folder)
            child, filestoreManager, eventManager));
      }
      return set.build();
    }

    @Override
    public FileOrFolder getFileOrFolder(final String name)
        throws RepositoryException {
      if (entity.getFolders().containsKey(name)) {
        return new Folder(
            entity.getFolders().get(name),
            filestoreManager,
            eventManager);
      }
      if (entity.getFiles().containsKey(name)) {
        return new File(
            entity.getFiles().get(name),
            filestoreManager,
            eventManager);
      }
      return null;
    }

    @Override
    public Set<FileStore.File> getFiles() throws RepositoryException {
      final ImmutableSet.Builder<FileStore.File> set =
          ImmutableSet.<FileStore.File> builder();
      for (final Object child : entity.getFiles().values()) {
        set.add(new File(
            (models.filestore.File) child,
            filestoreManager,
            eventManager));
      }
      return set.build();
    }

    public void resetPermissions() throws RepositoryException {
      final Group group = Admin.getInstance(session()).getGroup();
      final AorraAccessManager acm = (AorraAccessManager)session().getAccessControlManager();
      final Principal everyone = EveryonePrincipal.getInstance();
      acm.grant(everyone, FILE_STORE_PATH, jackrabbit.Permission.NONE);
      acm.grant(group.getPrincipal(), FILE_STORE_PATH, jackrabbit.Permission.RW);
    }

    @Override
    public boolean equals(final Object other) {
      if (other == null)
        return false;
      if (other instanceof Folder) {
        return ((Folder) other).getIdentifier().equals(getIdentifier());
      }
      return false;
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final FileStoreEvent event = FileStoreEvent.delete(this);
      filestoreManager.getFolderDAO().remove(rawPath());
      eventManager.tell(event, null);
    }

    @Override
    public String getIdentifier() {
      return entity.getId();
    }

    @Override
    public String getName() {
      return entity.getName();
    }

    @Override
    protected String rawPath() {
      return entity.getPath();
    }

    protected void reload() {
      entity = filestoreManager.getFolderDAO().get(rawPath());
    }

  }

  public static class File extends NodeWrapper<models.filestore.File> implements FileStore.File {

    protected File(models.filestore.File entity, Manager filestoreManager,
        ActorRef eventManager) throws RepositoryException {
      super(entity, filestoreManager, eventManager);
    }

    @Override
    public File update(final String mime, final InputStream data)
        throws RepositoryException {
      entity.setMimeType(mime);
      entity.setData(data);
      filestoreManager.getFileDAO().update(entity);
      eventManager.tell(FileStoreEvent.update(this), null);
      return this;
    }

    @Override
    public InputStream getData() {
      return entity.getDataProvider().getInputStream();
    }

    @Override
    public String getMimeType() {
      return entity.getMimeType();
    }

    @Override
    public boolean equals(final Object other) {
      if (other == null)
        return false;
      if (other instanceof File) {
        return ((File) other).getIdentifier().equals(getIdentifier());
      }
      return false;
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final FileStoreEvent event = FileStoreEvent.delete(this);
      filestoreManager.getFileDAO().remove(rawPath());
      eventManager.tell(event, null);
    }

    @Override
    public String getIdentifier() {
      return entity.getId();
    }

    @Override
    public SortedMap<String, service.filestore.FileStore.File> getVersions()
        throws RepositoryException {
      final ImmutableSortedMap.Builder<String,FileStore.File> b =
          ImmutableSortedMap.<String,FileStore.File>naturalOrder();
      for (models.filestore.File version :
          filestoreManager.getFileDAO().getVersionList(rawPath())) {
        b.put(version.getVersion(),
            new File(version, filestoreManager, eventManager));
      }
      return b.build();
    }

    @Override
    public User getAuthor() {
      return filestoreManager.getUserFromJackrabbitID(
          entity.getLastModifiedBy());
    }

    @Override
    public Calendar getModificationTime() {
      return entity.getLastModified();
    }

  }

  protected abstract static class NodeWrapper<T extends Child<models.filestore.Folder>> {

    protected final FileStoreImpl.Manager filestoreManager;
    protected final ActorRef eventManager;
    protected final Iterable<String> pathParts;
    protected T entity;

    protected NodeWrapper(
        T entity,
        final FileStoreImpl.Manager filestoreManager,
        final ActorRef eventManager) throws RepositoryException {
      this.filestoreManager = filestoreManager;
      this.eventManager = eventManager;
      if (entity == null)
        throw new NullPointerException("Underlying entity cannot be null.");
      this.entity = entity;
      this.pathParts = calculatePathParts();
    }

    public int getDepth() {
      return Iterables.size(pathParts) - 1;
    }

    public String getPath() {
      final String path = Joiner.on('/').join(pathParts);
      return path.isEmpty() ? "/" : path;
    }

    public Iterable<String> getPathParts() {
      return pathParts;
    }

    private Iterable<String> calculatePathParts() throws RepositoryException {
      if (getParent() == null)
        return Collections.singleton("");
      return Iterables.concat(
          ((NodeWrapper<?>) getParent()).getPathParts(),
          Collections.singleton(getName()));
    }

    public String getName() {
      return entity.getName();
    }

    protected String rawPath() {
      return entity.getPath();
    }

    protected Session session() throws RepositoryException {
      return filestoreManager.getSession();
    }

    public Map<String, Permission> getGroupPermissions()
        throws RepositoryException {
      final ImmutableMap.Builder<String, Permission> b = ImmutableMap
          .<String, Permission> builder();
      final Set<Group> groups =
          (new GroupManager(filestoreManager.getSession())).list();
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
      final JackrabbitSession session = (JackrabbitSession) session();
      final AccessControlPolicy[] policies = session.getAccessControlManager()
          .getEffectivePolicies(rawPath());
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

    public FileStore.Folder getParent() throws RepositoryException {
      if (rawPath().equals(FILE_STORE_PATH))
        return null;
      try {
        models.filestore.Folder parent = entity.getParent();
        if (parent == null) {
          parent = filestoreManager.getFolderDAO().get(
              PathUtils.getNode(rawPath(), filestoreManager.getSession())
                .getParent() /* files/folders */
                .getParent() /* parent */
                .getPath());
        }
        return new Folder(parent, filestoreManager, eventManager);
      } catch (NullPointerException npe) {
        // Parent returned as null
      } catch (JcrMappingException jme) {
        // Mapper had a fit with a null parent
      }
      return null;
    }

    @Override
    public int hashCode() {
      return entity.hashCode();
    }

  }

}
