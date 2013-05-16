package helpers;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;

public class FileStoreHelper {

  public static class FileExistsException extends Exception {
    private static final long serialVersionUID = 1L;
    public FileExistsException(FileStore.File f) {
      super(f.getPath());
    }
  }

  public static class FolderExistsException extends Exception {
    private static final long serialVersionUID = 1L;
    public FolderExistsException(FileStore.Folder f) {
      super(f.getPath());
    }
  }

  protected final Session session;

  public FileStoreHelper(final Session session) {
    this.session = session;
  }

  public void mkdir(final String absPath, final boolean createParents)
      throws FileExistsException, FolderExistsException, RepositoryException {
    final FileStore.Manager manager = fileStore().getManager(session);
    FileStore.Folder folder = manager.getRoot();
    final Iterator<String> iter = getPathParts(absPath).iterator();
    String name;
    while (iter.hasNext()) {
      name = iter.next();
      FileStore.FileOrFolder fof = folder.getFileOrFolder(name);
      if (fof == null) {
        // Only create if we can create parents or it's not a parent
        if (createParents || iter.hasNext()) {
          folder.createFolder(name);
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
