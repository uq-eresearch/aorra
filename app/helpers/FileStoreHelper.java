package helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

import play.Play;
import service.GuiceInjectionPlugin;
import service.filestore.FileStore;
import service.filestore.FileStoreImpl.Folder;
import service.filestore.roles.Admin;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;

public class FileStoreHelper {

  public abstract static class FileOrFolderException extends Exception {
    private static final long serialVersionUID = 1L;
    public FileOrFolderException(String msg) { super(msg); }
  }

  public static class FileExistsException extends FileOrFolderException {
    private static final long serialVersionUID = 1L;
    public FileExistsException(FileStore.File f) {
      super("File already exists: "+f.getPath());
    }
  }

  public static class FolderExistsException extends FileOrFolderException {
    private static final long serialVersionUID = 1L;
    public FolderExistsException(FileStore.Folder f) {
      super("Folder already exists: "+f.getPath());
    }
  }

  public static class FolderNotFoundException extends FileOrFolderException {
    private static final long serialVersionUID = 1L;
    public FolderNotFoundException(final String name) {
      super("Required folder does not exist: " + name);
    }
  }

  protected final Session session;

  protected final PrintWriter out;

  public FileStoreHelper(final Session session) {
    this(session, new PrintWriter(new OutputStreamWriter(System.out)));
  }

  public FileStoreHelper(final Session session, PrintWriter out) {
    this.session = session;
    this.out = out;
  }

  public FileStore.Folder mkdir(final String absPath, final boolean createParents)
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
    return folder;
  }

  public File createZipFile(final FileStore.Folder folder)
      throws IOException, RepositoryException {
    final File tempFile = File.createTempFile("zipfile", "");
    final ZipOutputStream zos =
        new ZipOutputStream(new FileOutputStream(tempFile));
    zos.setMethod(ZipOutputStream.DEFLATED);
    zos.setLevel(5);
    addFolderToZip(zos, folder, folder);
    zos.close();
    return tempFile;
  }

  /**
   * Get string to strip from the beginning of all zip paths, using the
   * speficied base folder.
   *
   * The specified folder's name should be preserved in the zip file, except
   * in the case of the root folder.
   *
   * @param folder Base folder to use
   * @return Prefix to strip from filestore paths
   */
  protected String getZipPath(
      final FileStore.FileOrFolder fof,
      final FileStore.Folder baseFolder) {
    if (baseFolder.getPath().equals("/")) {
      // Paths must not have a leading slash
      return fof.getPath().substring(1);
    } else {
      // To be user friendly, use single base directory
      return baseFolder.getName() + fof.getPath().replaceFirst(
        "^"+Pattern.quote(baseFolder.getPath()), "");
    }
  }

  protected void addFolderToZip(final ZipOutputStream zos,
      final FileStore.Folder folder,
      final FileStore.Folder baseFolder)
          throws IOException, RepositoryException {
    for (final FileStore.Folder subFolder : folder.getFolders()) {
      addFolderToZip(zos, subFolder, baseFolder);
    }
    for (final FileStore.File file : folder.getFiles()) {
      zos.putNextEntry(new ZipEntry(getZipPath(file, baseFolder)));
      InputStream data = file.getData();
      IOUtils.copy(data, zos);
      zos.closeEntry();
      data.close();
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
              sb.append(((FileStore.Folder)f).getGroupPermissions().toString());
          }
      }

      return sb.toString();
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

  private String makePath(String path1, String path2) {
      if(StringUtils.endsWith(path1, "/")) {
          return path1+path2;
      } else {
          return path1+"/"+path2;
      }
  }

  public void importArchive(String archive, String path) {
      if(StringUtils.isBlank(path)) {
          path = "/";
      }
      final FileStore.Manager manager = fileStore().getManager(session);
      File f = new File(archive);
      try {
          ZipInputStream zip = new ZipInputStream(new FileInputStream(f));
          while(true) {
              ZipEntry entry = zip.getNextEntry();
              if(entry == null) {
                  break;
              }
              String entrypath = entry.getName();
              String filename = FilenameUtils.getName(entrypath);
              String parent = makePath(path, FilenameUtils.getFullPathNoEndSeparator(entrypath));
              FileStore.Folder parentFolder = (Folder)manager.getFileOrFolder(parent);
              if(parentFolder == null) {
                  parentFolder = mkdir(parent, true);
              }
              if(!entry.isDirectory()) {
                  FileStore.File file = getFile(parentFolder, filename);
                  ByteArrayOutputStream bout = new ByteArrayOutputStream();
                  IOUtils.copy(zip, bout);
                  byte[] buf = bout.toByteArray();
                  if(file == null) {
                      parentFolder.createFile(filename, getMimetype(filename), new ByteArrayInputStream(buf));
                  } else {
                      out.println(String.format("file %s already exists", filename));
                  }
              }
          }
          zip.close();
      } catch(Exception e) {
          out.println(String.format("failed to import archive %s", archive));
          e.printStackTrace(out);
      }
  }

  private FileStore.File getFile(FileStore.Folder folder, String name) throws RepositoryException {
      for(FileStore.File file : folder.getFiles()) {
          if(file.getName().equals(name)) {
              return file;
          }
      }
      return null;
  }

  private String getMimetype(String filename) {
      MimeUtil2 mimeUtils = new MimeUtil2();
      mimeUtils.registerMimeDetector("eu.medsea.mimeutil.detector.ExtensionMimeDetector");
      MimeType m = MimeUtil2.getMostSpecificMimeType(mimeUtils.getMimeTypes(filename));
      return m!=null?m.toString():null;
  }

  protected FileStore fileStore() {
    return GuiceInjectionPlugin.getInjector(Play.application())
                               .getInstance(FileStore.class);
  }


}
