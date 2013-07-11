package service.filestore;

import jackrabbit.AorraAccessManager;

import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import models.GroupManager;
import models.User;
import models.UserDAO;
import models.filestore.Child;
import models.filestore.FileDAO;
import models.filestore.FolderDAO;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;
import org.jcrom.util.PathUtils;

import play.Logger;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.EventManager.FileStoreEvent;
import service.filestore.roles.Admin;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;

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

  private final EventManager eventManagerImpl;
  private final Jcrom jcrom;

  @Inject
  public FileStoreImpl(
      final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.jcrom = jcrom;
    Logger.debug(this+" - Creating file store.");
    final ActorSystem system = ActorSystem.create();
    eventManagerImpl =
        TypedActor.get(system).typedActorOf(
            new TypedProps<EventManagerImpl>(
                EventManager.class, EventManagerImpl.class));
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

  /* (non-Javadoc)
   * @see service.filestore.FileStore#getManager(javax.jcr.Session)
   */
  @Override
  public Manager getManager(final Session session) {
    return new Manager(session, jcrom, eventManagerImpl);
  }

  /* (non-Javadoc)
   * @see service.filestore.FileStore#getEventManager()
   */
  @Override
  public EventManager getEventManager() {
    return eventManagerImpl;
  }

  public static class Manager implements FileStore.Manager {

    private final Session session;
    private final Jcrom jcrom;
    private final EventManager eventManagerImpl;
    private final FileDAO fileDAO;
    private final FolderDAO folderDAO;
    private final UserDAO userDAO;
    private final Map<String, User> userCache =
        new HashMap<String, User>();

    protected Manager(final Session session, final Jcrom jcrom, final EventManager eventManagerImpl) {
      this.session = session;
      this.jcrom = jcrom;
      this.eventManagerImpl = eventManagerImpl;
      fileDAO = new FileDAO(session, jcrom);
      folderDAO = new FolderDAO(session, jcrom);
      userDAO = new UserDAO(session, jcrom);
    }

    @Override
    public FileOrFolder getByIdentifier(final String id)
        throws RepositoryException {
      try {
        return new Folder(getFolderDAO().loadById(id), this, eventManagerImpl);
      } catch (ClassCastException e) {
      } catch (JcrMappingException e) {
      } catch (NullPointerException e) {}
      try {
        return new File(getFileDAO().loadById(id), this, eventManagerImpl);
      } catch (ClassCastException e) {
      } catch (JcrMappingException e) {
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
        return getFolders(getRoot());
      } catch (NullPointerException e) {
        // Root probably doesn't exist
        return Collections.emptySet();
      }
    }

    private Set<FileStore.Folder> getFolders(final FileStore.Folder rootFolder)
        throws RepositoryException {
      if (rootFolder.getAccessLevel() != Permission.NONE) {
        return ImmutableSet.<FileStore.Folder>of(rootFolder);
      }
      final ImmutableSet.Builder<FileStore.Folder> b = ImmutableSet.builder();
      for (FileStore.Folder folder : rootFolder.getFolders()) {
        b.addAll(getFolders(folder));
      }
      return b.build();
    }

    @Override
    public FileStore.Folder getRoot() throws RepositoryException {
      return new FileStoreImpl.Folder(
          getFolderDAO().get(FILE_STORE_PATH), this, eventManagerImpl);
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
        EventManager eventManagerImpl) throws RepositoryException {
      super(entity, filestoreManager, eventManagerImpl);
    }

    @Override
    public Folder createFolder(final String name) throws RepositoryException {
      ensureDoesNotExist(name);
      final models.filestore.Folder newFolderEntity =
          filestoreManager.getFolderDAO().create(
              new models.filestore.Folder(entity, name));
      reload();
      final Folder folder = new Folder(
          newFolderEntity,
          filestoreManager,
          eventManagerImpl);
      eventManagerImpl.tell(FileStoreEvent.create(folder));
      return folder;
    }

    @Override
    public File createFile(final String name, final String mime,
        final InputStream data) throws RepositoryException {
      ensureDoesNotExist(name);
      final models.filestore.File newFileEntity =
        filestoreManager.getFileDAO().create(
            new models.filestore.File(entity, name, mime, data));
      final File file =
          new File(newFileEntity, filestoreManager, eventManagerImpl);
      reload();
      Logger.debug("New file, version "+newFileEntity.getVersion());
      eventManagerImpl.tell(FileStoreEvent.create(file));
      return file;
    }

    protected void ensureDoesNotExist(final String name)
        throws RepositoryException {
      final FileOrFolder fof = getFileOrFolder(name);
      if (fof != null)
        throw new ItemExistsException(String.format(
            "Can't create file '%s'. %s with same name already exists.",
            name, fof.getClass().getSimpleName()));
    }

    @Override
    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      final ImmutableSet.Builder<FileStore.Folder> set =
          ImmutableSet.<FileStore.Folder>builder();
      for (final Object child : entity.getFolders().values()) {
        set.add(new Folder((models.filestore.Folder)
            child, filestoreManager, eventManagerImpl));
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
            eventManagerImpl);
      }
      if (entity.getFiles().containsKey(name)) {
        return new File(
            entity.getFiles().get(name),
            filestoreManager,
            eventManagerImpl);
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
            eventManagerImpl));
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
      eventManagerImpl.tell(event);
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

    @Override
    public void grantAccess(String groupName, Permission permission)
        throws RepositoryException {
      final AorraAccessManager aam = (AorraAccessManager)
          session().getAccessControlManager();
      final Group group = (new GroupManager(session())).find(groupName);
      aam.grant(group.getPrincipal(),
          this.rawPath(), permission.toJackrabbitPermission());
    }

    @Override
    public void revokeAccess(final String groupName)
        throws RepositoryException {
      final AorraAccessManager aam = (AorraAccessManager)
          session().getAccessControlManager();
      aam.revoke(session().getWorkspace().getName(),
          groupName, this.getIdentifier());
    }

    @Override
    public Permission getAccessLevel() throws RepositoryException {
      final Permission onEntity = super.getAccessLevel();
      if (onEntity == Permission.RO) {
        // Exclude permissions based purely on ancestry by accessing child node
        // which should exist, but won't appear in those cases.
        if (!session().itemExists(rawPath()+"/files")) {
          return Permission.NONE;
        }
      }
      return onEntity;
    }

  }

  public static class File extends NodeWrapper<models.filestore.File> implements FileStore.File {

    private boolean hasRetrievedData;

    protected File(models.filestore.File entity, Manager filestoreManager,
        EventManager eventManagerImpl) throws RepositoryException {
      super(entity, filestoreManager, eventManagerImpl);
      this.hasRetrievedData = false;
    }

    @Override
    public File update(final String mime, final InputStream data)
        throws RepositoryException {
      entity.setMimeType(mime);
      entity.setData(data);
      filestoreManager.getFileDAO().update(entity);
      eventManagerImpl.tell(FileStoreEvent.update(this));
      return this;
    }

    @Override
    public InputStream getData() {
      // Unsafe to use the same underlying entity twice when getting an
      // input stream, so load again if necessary.
      final models.filestore.File f = this.hasRetrievedData ?
          filestoreManager.getFileDAO().get(rawPath()) :
          entity;
      this.hasRetrievedData = true;
      return f.getDataProvider().getInputStream();
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
      eventManagerImpl.tell(event);
    }

    @Override
    public String getIdentifier() {
      return entity.getId();
    }

    @Override
    public FileStore.File getLatestVersion()
        throws RepositoryException {
      return new File(
          filestoreManager.getFileDAO().getVersion(rawPath(),
              entity.getLatestVersion()),
          filestoreManager, eventManagerImpl);
    }

    @Override
    public SortedMap<String, service.filestore.FileStore.File> getVersions()
        throws RepositoryException {
      final ImmutableSortedMap.Builder<String,FileStore.File> b =
          ImmutableSortedMap.<String,FileStore.File>naturalOrder();
      for (models.filestore.File version :
          filestoreManager.getFileDAO().getVersionList(rawPath())) {
        b.put(version.getVersion(),
            new File(version, filestoreManager, eventManagerImpl));
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
    protected final EventManager eventManagerImpl;
    protected final Iterable<String> pathParts;
    protected T entity;

    protected NodeWrapper(
        T entity,
        final FileStoreImpl.Manager filestoreManager,
        final EventManager eventManagerImpl) throws RepositoryException {
      this.filestoreManager = filestoreManager;
      this.eventManagerImpl = eventManagerImpl;
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
      AorraAccessManager aam = (AorraAccessManager)session().getAccessControlManager();
      Map<Principal, jackrabbit.Permission> permissions = aam.getPermissions(rawPath());
      for(Map.Entry<Principal, jackrabbit.Permission> me : permissions.entrySet()) {
        b.put(me.getKey(), Permission.fromJackrabbitPermission(me.getValue()));
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
        return new Folder(parent, filestoreManager, eventManagerImpl);
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

    public Permission getAccessLevel() throws RepositoryException {
      try {
        session().checkPermission(FILE_STORE_PATH, "read");
      } catch (AccessControlException e) {
        return Permission.NONE;
      }
      try {
        session().checkPermission(FILE_STORE_PATH, "set_property");
      } catch (AccessControlException e) {
        return Permission.RO;
      }
      return Permission.RW;
    }

  }

}
