package helpers;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;
import service.filestore.roles.Admin;

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

  protected final PrintWriter out;

  public FileStoreHelper(final Session session) {
    this.session = session;
    this.out = new PrintWriter(new OutputStreamWriter(System.out));
  }
  
  public FileStoreHelper(final Session session, PrintWriter out) {
    this.session = session;
    this.out = out;
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
          out.println(String.format("Creating new folder in %s: %s",
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

  public void rm(final String path, final boolean recursive) throws RepositoryException {
      if(path == null) {
          return;
      }
      FileStore.FileOrFolder f = fileStore().getManager(session).getFileOrFolder(path);
      if(f == null) {
          out.println(String.format("no such file or directory '%s'", path));
      } else if(f instanceof FileStore.Folder && !recursive) {
          out.println(String.format(
                  "cannot remove '%s': Is a directory, try using --recursive", path));
      } else {
          f.delete();
      }
  }

  private String format(FileStore.FileOrFolder f, boolean isFolder, boolean showPerms) throws RepositoryException {
      StringBuilder sb = new StringBuilder();
      for(int i = 0;i<f.getDepth()-1;i++) {
          sb.append("|   ");
      }
      if(StringUtils.equals(f.getPath(), "/")) {
          sb.append("/ (root)");
      } else {
          sb.append("|");
          sb.append("-");
          if(isFolder) {
              sb.append("+");
          } else {
              sb.append("-");
          }
          sb.append(" ");
          sb.append(f.getName());
          if(showPerms && f instanceof FileStore.Folder) {
              sb.append(" ");
              sb.append(mapToString(((FileStore.Folder)f).getGroupPermissions()));
          }
      }
      
      return sb.toString();
  }

  @SuppressWarnings("rawtypes")
  private String mapToString(Map map) {
      StringBuilder sb = new StringBuilder();
      for(Object me : map.entrySet()) {
          Object key = ((Map.Entry)me).getKey();
          Object val = ((Map.Entry)me).getValue();
          sb.append(ObjectUtils.toString(key, "(null)"));
          sb.append(" -> ");
          sb.append(ObjectUtils.toString(val, "(null)"));
      }
      return "";
  }

  private void tree(final FileStore.Folder folder, boolean showPerms) throws RepositoryException {
    out.println(format(folder, true, showPerms));
    for(FileStore.Folder f: folder.getFolders()) {
        tree(f, showPerms);
    }
    for(FileStore.File f : folder.getFiles()) {
        out.println(format(f, false, showPerms));
    }
  }

  public void tree(final String path, Boolean showPerms) throws RepositoryException {
      boolean permissions = false;
      if(showPerms != null) {
          permissions = showPerms.booleanValue();
      }
      FileStore.Manager m = fileStore().getManager(session);
      FileStore.FileOrFolder f = m.getFileOrFolder(StringUtils.isBlank(path) ? "/" : path);
      if (f == null) {
        out.println(String.format("no such folder %s", path));
      } else if (f instanceof FileStore.File) {
        out.println(String.format("%s is a file", path));
      } else {
        tree((FileStore.Folder) f, permissions);
      }
  }

  private void listAdminTree(Authorizable authorizable, int depth) throws RepositoryException {
      boolean isGroup = authorizable instanceof Group;
      String id = authorizable.getID();
      String email = "";
      try {
          // Get user email if user
          Node node = session.getNodeByIdentifier(id);
          email = node.getProperty("email").getValue().getString();
        } catch (Exception e) {}
      StringBuilder sb = new StringBuilder();
      for(int i = 0;i<depth-1;i++) {
          sb.append("|   ");
      }
      if(isGroup) {
          sb.append("|-+ ");
          sb.append(id);
      } else {
          sb.append("|-- ");
          if(StringUtils.isNotBlank(email)) {
              sb.append(email);
          } else {
              sb.append(id);
          }
      }
      out.println(sb);
      if(isGroup) {
          Iterator<Authorizable> aIter = ((Group)authorizable).getDeclaredMembers();
          while(aIter.hasNext()) {
              Authorizable a = aIter.next();
              listAdminTree(a, depth + 1);
          }
      }
  }

  public void listadmin(String name) throws RepositoryException {
      Group root = Admin.getInstance(session).getGroup();
      listAdminTree(root, 0);
  }

  protected FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(Play.application())
                               .getInstance(FileStore.class);
  }


}
