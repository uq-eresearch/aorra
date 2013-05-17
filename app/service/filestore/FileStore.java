package service.filestore;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import akka.actor.ActorRef;

public interface FileStore {

  public enum Permission {
    NONE, RO, RW;

    public boolean isAtLeast(Permission other) {
      return this.compareTo(other) >= 0;
    }

  }

  Manager getManager(Session session);

  ActorRef getEventManager();

  static interface Manager {

    Folder getRoot() throws RepositoryException;

    FileOrFolder getFileOrFolder(final String absPath) throws RepositoryException;

    Set<Folder> getFolders() throws RepositoryException;

  }

  static interface FileOrFolder {

    String getIdentifier(); /* Globally unique */

    String getName(); /* Unique only inside parent folder */

    int getDepth();

    String getPath(); /* Globally unique */

    void delete() throws RepositoryException;

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

  }

  static interface File extends FileOrFolder {

    InputStream getData();

    String getMimeType();

    File update(String mime, InputStream data) throws RepositoryException;

  }


}