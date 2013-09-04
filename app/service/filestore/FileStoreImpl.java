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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.IdentifierResolver;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.jcrom.Jcrom;
import org.jcrom.dao.JcrDAO;
import org.jcrom.util.PathUtils;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Function;
import service.JcrSessionFactory;
import service.filestore.EventManager.Event;
import service.filestore.roles.Admin;
import akka.actor.TypedActor;
import akka.actor.TypedProps;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class FileStoreImpl implements FileStore {

  public static final String FILE_STORE_PATH = "/filestore";
  public static final String FILE_STORE_NODE_NAME =
      StringUtils.stripStart(FILE_STORE_PATH, "/");
  public static final String FILE_STORE_NAME = "AORRA";

  private final EventManager eventManagerImpl;
  private final Jcrom jcrom;

  @Inject
  public FileStoreImpl(
      final JcrSessionFactory sessionFactory,
      final Jcrom jcrom) {
    this.jcrom = jcrom;
    Logger.debug(this+" - Creating file store.");
    eventManagerImpl =
        TypedActor.get(Akka.system()).typedActorOf(
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

    private final Cache<models.filestore.File, FileStore.File> fileCache =
        CacheBuilder.newBuilder().build();
    private final Cache<models.filestore.Folder, FileStore.Folder> folderCache =
        CacheBuilder.newBuilder().build();
    private FileStore.Folder rootFolder;

    protected Manager(final Session session, final Jcrom jcrom, final EventManager eventManagerImpl) {
      this.session = session;
      this.jcrom = jcrom;
      this.eventManagerImpl = eventManagerImpl;
      fileDAO = new FileDAO(session, jcrom);
      folderDAO = new FolderDAO(session, jcrom);
      userDAO = new UserDAO(session, jcrom);
    }

    protected FileStore.Folder wrap(
        final models.filestore.Folder entity,
        final FileStore.Folder parent) {
      final FileStoreImpl.Manager fm = this;
      try {
        return folderCache.get(entity, new Callable<FileStore.Folder>() {
          @Override
          public FileStore.Folder call() throws RepositoryException {
            return new FileStoreImpl.Folder(entity,
                parent, fm, eventManagerImpl);
          }
        });
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getCause());
      }
    }

    protected FileStore.File wrap(
        final models.filestore.File entity,
        final FileStore.Folder parent) {
      final FileStoreImpl.Manager fm = this;
      try {
        return fileCache.get(entity, new Callable<FileStore.File>() {
          @Override
          public FileStore.File call() throws RepositoryException {
            return new FileStoreImpl.File(entity, parent,
                fm, eventManagerImpl);
          }
        });
      } catch (ExecutionException e) {
        throw new RuntimeException(e.getCause());
      }
    }

    protected models.filestore.Folder reload(
        final models.filestore.Folder entity) {
      // Get existing value
      final FileStore.Folder f = folderCache.getIfPresent(entity);
      // Get new key
      final models.filestore.Folder newEntity =
          getFolderDAO().loadById(entity.getId());
      folderCache.invalidate(entity);
      folderCache.put(newEntity, f);
      return newEntity;
    }

    protected models.filestore.File reload(final models.filestore.File entity) {
      // Get existing value
      final FileStore.File f = fileCache.getIfPresent(entity);
      // Get new key
      final models.filestore.File newEntity =
          getFileDAO().loadById(entity.getId());
      fileCache.invalidate(entity);
      fileCache.put(newEntity, f);
      return newEntity;
    }

    @Override
    public FileOrFolder getByIdentifier(final String id)
        throws RepositoryException {
      final String absPath = NodeWrapper.getPath(session, id);
      if (absPath == null)
        return null;
      return getFileOrFolder(absPath);
    }

    @Override
    public FileOrFolder getFileOrFolder(final String absPath)
        throws RepositoryException {
      // We want a new copy
      getRoot().reload();
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
        folder = (service.filestore.FileStore.Folder) fof;
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
      if (rootFolder == null) {
        rootFolder = wrap(getFolderDAO().get(FILE_STORE_PATH), null);
      }
      return rootFolder;
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

    private Set<FileStore.Folder> folders = null;
    private Set<FileStore.File> files = null;

    protected Folder(models.filestore.Folder entity,
        FileStore.Folder parent,
        Manager filestoreManager,
        EventManager eventManagerImpl) throws RepositoryException {
      super(entity, parent, filestoreManager, eventManagerImpl);
    }

    @Override
    public void reload() {
      entity = filestoreManager.reload(entity);
      // Clear local collection caches
      folders = null;
      files = null;
    }

    @Override
    public FileStore.Folder createFolder(final String name)
        throws RepositoryException {
      ensureDoesNotExist(name);
      final models.filestore.Folder newFolderEntity =
          getDAO().create(
              new models.filestore.Folder(entity, name));
      final FileStore.Folder folder =
          filestoreManager.wrap(newFolderEntity, this);
      getMutableFolderSet().add(folder);
      eventManagerImpl.tell(Event.create(folder));
      return folder;
    }

    @Override
    public FileStore.File createFile(final String name, final String mime,
        final InputStream data) throws RepositoryException {
      ensureDoesNotExist(name);
      final models.filestore.File newFileEntity =
        filestoreManager.getFileDAO().create(
            new models.filestore.File(entity, name, mime, data));
      final FileStore.File file = filestoreManager.wrap(newFileEntity, this);
      getMutableFileSet().add(file);
      Logger.debug("New file, version "+newFileEntity.getVersion());
      eventManagerImpl.tell(Event.create(file));
      return file;
    }

    protected void ensureDoesNotExist(final String name)
        throws RepositoryException {
      reload();
      final FileOrFolder fof = getFileOrFolder(name);
      if (fof != null)
        throw new ItemExistsException(String.format(
            "Can't create file '%s'. %s with same name already exists.",
            name, fof.getClass().getSimpleName()));
    }

    @Override
    public Set<FileStore.Folder> getFolders() throws RepositoryException {
      return ImmutableSet.copyOf(getMutableFolderSet());
    }

    public Set<FileStore.Folder> getMutableFolderSet()
        throws RepositoryException {
      if (folders == null) {
        final Set<FileStore.Folder> set = Sets.newHashSet();
        for (final Object child : entity.getFolders().values()) {
          set.add(filestoreManager.wrap((models.filestore.Folder) child, this));
        }
        folders = set;
      }
      return folders;
    }

    @Override
    public FileOrFolder getFileOrFolder(final String name)
        throws RepositoryException {
      for (FileStore.Folder folder : getFolders()) {
        if (folder.getName().equals(name)) {
          return folder;
        }
      }
      for (FileStore.File file : getFiles()) {
        if (file.getName().equals(name)) {
          return file;
        }
      }
      return null;
    }

    @Override
    public Set<FileStore.File> getFiles() throws RepositoryException {
      return ImmutableSet.copyOf(getMutableFileSet());
    }

    protected Set<FileStore.File> getMutableFileSet()
        throws RepositoryException {
      if (files == null) {
        final Set<FileStore.File> set = Sets.newHashSet();
        for (final Object child : entity.getFiles().values()) {
          set.add(filestoreManager.wrap((models.filestore.File) child, this));
        }
        files = set;
      }
      return files;
    }

    public void resetPermissions() throws RepositoryException {
      final Group group = Admin.getInstance(session()).getGroup();
      final AorraAccessManager acm = (AorraAccessManager)session().getAccessControlManager();
      final Principal everyone = EveryonePrincipal.getInstance();
      acm.grant(everyone, FILE_STORE_PATH, jackrabbit.Permission.NONE);
      acm.grant(group.getPrincipal(), FILE_STORE_PATH, jackrabbit.Permission.RW);
    }

    @Override
    public void rename(final String newName)
        throws ItemExistsException, RepositoryException {
      super.rename(newName);
      eventManagerImpl.tell(Event.updateFolder(getIdentifier()));
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final Event event = Event.delete(this);
      getDAO().remove(rawPath());
      eventManagerImpl.tell(event);
    }

    @Override
    public String getIdentifier() {
      return entity.getId();
    }

    @Override
    protected String rawPath() {
      return entity.getPath();
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

    @Override
    public JcrDAO<models.filestore.Folder> getDAO() {
      return filestoreManager.getFolderDAO();
    }

  }

  public static class File extends NodeWrapper<models.filestore.File> implements FileStore.File {

    private boolean hasRetrievedData;

    protected File(models.filestore.File entity,
        FileStore.Folder parent,
        Manager filestoreManager,
        EventManager eventManagerImpl) throws RepositoryException {
      super(entity, parent, filestoreManager, eventManagerImpl);
      this.hasRetrievedData = false;
    }

    @Override
    protected void reload() {
      entity = filestoreManager.reload(entity);
    }

    @Override
    public File update(final String mime, final InputStream data)
        throws RepositoryException {
      entity.setMimeType(mime);
      entity.setData(data);
      getDAO().update(entity);
      eventManagerImpl.tell(Event.update(this));
      return this;
    }

    @Override
    public InputStream getData() {
      // Unsafe to use the same underlying entity twice when getting an
      // input stream, so load again if necessary.
      final models.filestore.File f = this.hasRetrievedData ?
          getDAO().get(rawPath()) : entity;
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
    public void rename(final String newName)
        throws ItemExistsException, RepositoryException {
      super.rename(newName);
      eventManagerImpl.tell(Event.update(this));
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final Event event = Event.delete(this);
      getDAO().remove(rawPath());
      eventManagerImpl.tell(event);
    }

    @Override
    public String getIdentifier() {
      return entity.getId();
    }

    @Override
    public FileStore.File getLatestVersion()
        throws RepositoryException {
      return new FileVersion(this,
          getDAO().getVersion(rawPath(), entity.getLatestVersion()),
          filestoreManager, eventManagerImpl);
    }

    protected String getLatestVersionName() {
      return entity.getLatestVersion();
    }

    @Override
    public SortedMap<String, service.filestore.FileStore.File> getVersions()
        throws RepositoryException {
      final ImmutableSortedMap.Builder<String,FileStore.File> b =
          ImmutableSortedMap.<String,FileStore.File>naturalOrder();
      final List<models.filestore.File> versions =
          getDAO().getVersionListById(entity.getId());
      models.filestore.File lastVersion = null;
      for (models.filestore.File version : versions) {
        if (version.containsSameDataAs(lastVersion))
          continue;
        b.put(version.getVersion(), new FileVersion(this,
            version, filestoreManager, eventManagerImpl));
        lastVersion = version;
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

    @Override
    public JcrDAO<models.filestore.File> getDAO() {
      return filestoreManager.getFileDAO();
    }

    @Override
    public String toString() {
      return "File [getIdentifier()=" + getIdentifier() + ", getPath()="
          + getPath() + "]";
    }

  }

  protected static class FileVersion extends File {

    protected final File file;

    protected FileVersion(
        File file,
        models.filestore.File versionEntity,
        Manager filestoreManager, EventManager eventManagerImpl)
        throws RepositoryException {
      super(versionEntity, null, filestoreManager, eventManagerImpl);
      this.file = file;
    }

    @Override
    public FileStore.Folder getParent() {
      throw new NotImplementedException();
    }

    @Override
    protected void reload() {
      throw new NotImplementedException();
    }

    @Override
    public FileStore.File getLatestVersion()
        throws RepositoryException {
      throw new NotImplementedException();
    }

    @Override
    public SortedMap<String, service.filestore.FileStore.File> getVersions() {
      throw new NotImplementedException();
    }

    @Override
    public void delete() throws AccessDeniedException, VersionException,
        LockException, ConstraintViolationException, RepositoryException {
      final Event event = Event.delete(this);
      if (entity.getVersion().equals(file.getLatestVersionName())) {
        final List<models.filestore.File> versions =
            getDAO().getVersionListById(file.getIdentifier());
        if (versions.size() < 2) {
          throw new AccessDeniedException("You can't remove the last version!");
        }
        getDAO().restoreVersionById(file.getIdentifier(),
            versions.get(versions.size()-2).getVersion(), true);
      }
      getDAO().removeVersionById(file.getIdentifier(), entity.getVersion());
      eventManagerImpl.tell(event);
      file.reload();
    }

  }

  protected abstract static class NodeWrapper<T extends Child<models.filestore.Folder>> {

    protected final FileStoreImpl.Manager filestoreManager;
    protected final EventManager eventManagerImpl;
    protected Iterable<String> pathParts;
    protected T entity;
    private final FileStore.Folder parent;

    protected NodeWrapper(
        T entity,
        final FileStore.Folder parent,
        final FileStoreImpl.Manager filestoreManager,
        final EventManager eventManagerImpl) throws RepositoryException {
      this.parent = parent;
      this.filestoreManager = filestoreManager;
      this.eventManagerImpl = eventManagerImpl;
      if (entity == null)
        throw new NullPointerException("Underlying entity cannot be null.");
      this.entity = entity;
      updatePath();
    }

    public static String getPath(Session session, String id)
        throws RepositoryException {
      try {
        return getPathFromParts(getPathParts(session, id));
      } catch (MalformedPathException e) {
        // Node with identifier doesn't exist
        return null;
      }
    }

    protected static Iterable<String> getPathParts(Session session, String id)
        throws RepositoryException {
      final IdentifierResolver resolver = (IdentifierResolver) session;
      final Path rootPath = resolver.getPath(session.getNode(FILE_STORE_PATH)
          .getIdentifier());
      return calculatePathParts(
          rootPath.computeRelativePath(resolver.getPath(id)));
    }

    // Calculate parts from relative path
    private static Iterable<String> calculatePathParts(Path path)
        throws RepositoryException {
      if (path.getDepth() <= 0)
        return Collections.singleton("");
      return Iterables.concat(
          calculatePathParts(path.getAncestor(2)), // Skip file/folder container
          Collections.singleton(path.getName().getLocalName()));
    }

    public int getDepth() {
      return Iterables.size(pathParts) - 1;
    }

    private static String getPathFromParts(Iterable<String> pathParts) {
      final String path = Joiner.on('/').join(pathParts);
      return path.isEmpty() ? "/" : path;
    }

    public String getPath() {
      return getPathFromParts(pathParts);
    }

    public Iterable<String> getPathParts() {
      return pathParts;
    }

    /**
     * Used after moving or renaming.
     * @throws RepositoryException
     */
    protected void updatePath() throws RepositoryException {
      this.pathParts = calculatePathParts();
    }

    private Iterable<String> calculatePathParts() throws RepositoryException {
      return getPathParts(filestoreManager.getSession(), entity.getId());
    }

    public String getName() {
      if (rawPath().equals(FILE_STORE_PATH))
        return FILE_STORE_NAME;
      return entity.getName();
    }

    protected String rawPath() {
      return entity.getPath();
    }

    protected abstract void reload();

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
      return parent;
    }

    public void rename(final String newName)
        throws ItemExistsException, RepositoryException {
      if (getParent() == null)
        throw new ItemExistsException("Can't rename root folder.");
      {
        final FileStore.FileOrFolder fof = getParent().getFileOrFolder(newName);
        if (fof != null)
          throw new ItemExistsException(String.format(
              "Can't rename '%s' to '%s'. %s with same name already exists.",
              getName(), newName, fof.getClass().getSimpleName()));
      }
      entity.setName(newName);
      getDAO().update(entity, "none", 0);
      // We cache the path, so it has to be updated
      updatePath();
    }

    public abstract JcrDAO<T> getDAO();

    @Override
    public int hashCode() {
      return entity.hashCode();
    }

    public Permission getAccessLevel() throws RepositoryException {
      try {
        session().checkPermission(rawPath(), "read");
      } catch (AccessControlException e) {
        return Permission.NONE;
      }
      try {
        session().checkPermission(rawPath(), "set_property");
      } catch (AccessControlException e) {
        return Permission.RO;
      }
      return Permission.RW;
    }

  }

}
