package service.filestore;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import service.EventManager;
import service.EventManager.Event;
import service.EventManager.Event.NodeInfo;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import models.User;

public interface FileStore {

  public enum Permission {
    NONE, RO, RW;

    private static BiMap<Permission, jackrabbit.Permission> jackrabbitPermMap =
        ImmutableBiMap.<Permission, jackrabbit.Permission>builder()
          .put(Permission.NONE, jackrabbit.Permission.NONE)
          .put(Permission.RO, jackrabbit.Permission.RO)
          .put(Permission.RW, jackrabbit.Permission.RW)
          .build();

    public boolean isAtLeast(Permission other) {
      return this.compareTo(other) >= 0;
    }

    public jackrabbit.Permission toJackrabbitPermission() {
      return jackrabbitPermMap.get(this);
    }

    public static Permission fromJackrabbitPermission(jackrabbit.Permission o) {
      return jackrabbitPermMap.inverse().get(o);
    }

  }

  Manager getManager(Session session);

  EventManager getEventManager();

  static interface Manager {

    Folder getRoot() throws RepositoryException;

    FileOrFolder getByIdentifier(final String id) throws RepositoryException;

    FileOrFolder getFileOrFolder(final String absPath) throws RepositoryException;

    Set<Folder> getFolders() throws RepositoryException;

  }

  static interface FileOrFolder {

    String getIdentifier(); /* Globally unique */

    String getName(); /* Unique only inside parent folder */

    int getDepth();

    String getPath(); /* Globally unique */

    Folder getParent() throws RepositoryException;

    void delete() throws RepositoryException;

    /**
     * Rename this file/folder.
     *
     * @param newName Name following rename
     * @throws ItemExistsException if another file/folder exists
     * @throws RepositoryException
     */
    void rename(String newName)
        throws ItemExistsException, RepositoryException;

    /**
     * Determine access level to this item. Note that this may vary from the
     * access to the node, as permissions based soley on ancestory to a child
     * node should be excluded.
     *
     * @return
     * @throws RepositoryException
     */
    Permission getAccessLevel() throws RepositoryException;

  }

  static interface Folder extends FileOrFolder {

    Folder createFolder(String name) throws RepositoryException;

    File createFile(String name, String mime, InputStream data)
        throws RepositoryException;

    FileOrFolder getFileOrFolder(String name) throws RepositoryException;

    Set<File> getFiles() throws RepositoryException;

    Set<Folder> getFolders() throws RepositoryException;

    Map<String, Permission> getGroupPermissions()
        throws RepositoryException;

    void grantAccess(String groupName, Permission permission)
        throws RepositoryException;

    void revokeAccess(String groupName)
        throws RepositoryException;

    void reload();

  }

  static interface File extends FileOrFolder {

    InputStream getData();

    String getMimeType();

    File getLatestVersion() throws RepositoryException;

    SortedMap<String,File> getVersions() throws RepositoryException;

    File update(String mime, InputStream data) throws RepositoryException;

    User getAuthor();

    Calendar getModificationTime();

  }

  public static class Events {

    public static Event create(FileStore.File file)
        throws RepositoryException {
      return new Event("file:create", nodeInfo(file));
    }

    public static Event create(FileStore.Folder folder)
        throws RepositoryException {
      return new Event("folder:create", nodeInfo(folder));
    }

    public static Event update(FileStore.File file)
        throws RepositoryException {
      return new Event("file:update", nodeInfo(file));
    }

    public static Event updateFolder(String folderId)
        throws RepositoryException {
      return new Event("folder:update", new NodeInfo(folderId));
    }

    public static Event delete(FileStore.File file)
        throws RepositoryException {
      return new EventManager.Event("file:delete", nodeInfo(file));
    }

    public static Event delete(FileStore.Folder folder)
        throws RepositoryException {
      return new EventManager.Event("folder:delete", nodeInfo(folder));
    }

    private static Event.NodeInfo nodeInfo(FileStore.Folder folder) {
      return new Event.NodeInfo(folder.getIdentifier());
    }

    private static Event.NodeInfo nodeInfo(FileStore.File file) {
      return new Event.NodeInfo(file.getIdentifier());
    }

  }


}