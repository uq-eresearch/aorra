package helpers;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import play.Logger;
import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;

public class FileStoreHelper {

  public static class FileExistsException extends Exception {
    private static final long serialVersionUID = 1L;
    public FileExistsException(FileStore.File f) {
      super("File already exists: "+f.getPath());
    }
  }

  public static class FolderExistsException extends Exception {
    private static final long serialVersionUID = 1L;
    public FolderExistsException(FileStore.Folder f) {
      super("Folder already exists: "+f.getPath());
    }
  }

  public static class FolderNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;
    public FolderNotFoundException(final String name) {
      super("Required folder does not exist: " + name);
    }
  }

  protected final Session session;

  public FileStoreHelper(final Session session) {
    this.session = session;
  }

  public void mkdir(final String absPath, final boolean createParents)
      throws FileExistsException, FolderExistsException,
      FolderNotFoundException, RepositoryException {
    final FileStore.Manager manager = fileStore().getManager(session);
    FileStore.Folder folder = manager.getRoot();
    final Iterator<String> iter = getPathParts(absPath).iterator();
    String name;
    while (iter.hasNext()) {
      name = iter.next();
      FileStore.FileOrFolder fof = folder.getFileOrFolder(name);
      if (fof == null) {
        // Only create if we can create parents or it's not a parent
        if (createParents || !iter.hasNext()) {
          Logger.debug(String.format("Creating new folder in %s: %s",
              folder.getPath(), name));
          folder = folder.createFolder(name);
        } else {
          throw new FolderNotFoundException(name);
        }
      } else if (fof instanceof FileStore.Folder) {
        folder = (FileStore.Folder) fof;
        // Folder already exists
        if (!iter.hasNext())
          throw new FolderExistsException(folder);
      } else {
        throw new FileExistsException((FileStore.File) fof);
      }
    }
  }

  protected Iterable<String> getPathParts(final String absPath) {
    final Deque<String> q = new LinkedList<String>();
    java.io.File f = new java.io.File(absPath);
    do {
      if (!f.getName().equals(""))
        q.addFirst(f.getName());
      f = f.getParentFile();
    } while (f != null);
    return q;
  }

  protected FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(Play.application())
                               .getInstance(FileStore.class);
  }


}
