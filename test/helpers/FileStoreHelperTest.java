package helpers;

import static com.google.common.collect.Collections2.orderedPermutations;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.jcrom;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.jcr.Session;

import models.GroupManager;
import models.User;
import models.UserDAO;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.jackrabbit.api.security.user.Group;
import org.eclipse.jetty.util.IO;
import org.junit.Test;

import play.libs.F;
import service.filestore.FileStore;
import service.filestore.roles.Admin;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class FileStoreHelperTest {

  @Test
  public void mkdirSingle() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath = "/foobar";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
          assertThat(fof).isNull();
        }
        fsh.mkdir(absPath, false);
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
          assertThat(fof).isNotNull();
          assertThat(fof).isInstanceOf(FileStore.Folder.class);
        }
        return null;
      }
    });
  }

  @Test
  public void mkdirSingleWhenFolderExists() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath = "/foobar";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
          assertThat(fof).isNull();
          fm.getRoot().createFolder("foobar");
        }
        try {
          fsh.mkdir(absPath, false);
          fail("Should have thrown FolderExistsException");
        } catch (FileStoreHelper.FolderExistsException e) {
          assertThat(e.getMessage()).contains("foobar");
        }
        return null;
      }

    });
  }

  @Test
  public void mkdirSingleWhenFileExists() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath = "/foobar";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
          assertThat(fof).isNull();
          fm.getRoot().createFile("foobar", "text/plain",
              new ByteArrayInputStream("Some content.".getBytes()));
        }
        try {
          fsh.mkdir(absPath, false);
          fail("Should have thrown FileExistsException");
        } catch (FileStoreHelper.FileExistsException e) {
          assertThat(e.getMessage()).contains("foobar");
        }
        return null;
      }

    });
  }

  @Test
  public void mkdirMultiple() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath = "/foo/bar";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
          assertThat(fof).isNull();
        }
        try {
          fsh.mkdir(absPath, false);
          fail("Should have thrown FolderNotFoundException");
        } catch (FileStoreHelper.FolderNotFoundException e) {
          assertThat(e.getMessage()).contains("foo");
        }
        return null;
      }
    });
  }

  @Test
  public void mkdirMultipleWithParentCreation() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath1 = "/foo/bar";
        final String absPath2 = "/foo/baz";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
          assertThat(fof).isNull();
        }
        fsh.mkdir(absPath1, true);
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
          assertThat(fof).isNotNull();
          assertThat(fof).isInstanceOf(FileStore.Folder.class);
        }
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath2);
          assertThat(fof).isNull();
        }
        fsh.mkdir(absPath2, true);
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath2);
          assertThat(fof).isNotNull();
          assertThat(fof).isInstanceOf(FileStore.Folder.class);
        }
        return null;
      }
    });
  }

  @Test
  public void createZipFile() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fh,
          final FileStore.Manager fm) throws Throwable {
        fh.mkdir("/a (a-f)/1", true);
        fh.mkdir("/a (a-f)/2", true);
        fh.mkdir("/b (a-f)/1", true);
        final byte[] testBytes = "Some test content.".getBytes();
        ((FileStore.Folder) fm.getFileOrFolder("/a (a-f)/2"))
          .createFile("test.txt", "text/plain",
              new ByteArrayInputStream(testBytes));
        // Test root folder
        {
          final java.io.File tf = asTempFile(fh.createZipFile(fm.getRoot()));
          assertThat(tf).isNotNull();
          final ZipFile zf = new ZipFile(tf);
          final Enumeration<? extends ZipEntry> entries = zf.entries();
          final ZipEntry ze = entries.nextElement();
          assertThat(ze).isNotNull();
          assertThat(ze.getName()).isEqualTo("a (a-f)/2/test.txt");
          assertThat(ze.getSize()).isEqualTo(testBytes.length);
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          IOUtils.copy(zf.getInputStream(ze), bos);
          assertThat(bos.toByteArray()).isEqualTo(testBytes);
          assertThat(entries.hasMoreElements()).isFalse();
          zf.close();
        }
        // Test subfolder
        {
          final java.io.File tf = asTempFile(fh.createZipFile(
              (FileStore.Folder) fm.getFileOrFolder("/a (a-f)/2")));
          assertThat(tf).isNotNull();
          final ZipFile zf = new ZipFile(tf);
          final Enumeration<? extends ZipEntry> entries = zf.entries();
          final ZipEntry ze = entries.nextElement();
          assertThat(ze).isNotNull();
          assertThat(ze.getName()).isEqualTo("2/test.txt");
          assertThat(ze.getSize()).isEqualTo(testBytes.length);
          final ByteArrayOutputStream bos = new ByteArrayOutputStream();
          IOUtils.copy(zf.getInputStream(ze), bos);
          assertThat(bos.toByteArray()).isEqualTo(testBytes);
          assertThat(entries.hasMoreElements()).isFalse();
          zf.close();
        }
        return session;
      }
    });
  }

  @Test
  public void rmFile() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String filename = "test.txt";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+filename);
          assertThat(fof).isNull();
        }
        {
          fm.getRoot().createFile(filename, "text/plain",
              new ByteArrayInputStream("Hello World!".getBytes()));
          final FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+filename);
          assertThat(fof).isNotNull();
        }
        fsh.rm("/"+filename, true);
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+filename);
          assertThat(fof).isNull();
        }
        return null;
      }
    });
  }

  @Test
  public void rmFolder() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath1 = "/foo/bar";
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
          assertThat(fof).isNull();
        }
        fsh.mkdir(absPath1, true);
        {
          final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
          assertThat(fof).isNotNull();
          assertThat(fof).isInstanceOf(FileStore.Folder.class);
        }
        fsh.rm(absPath1, true);
        {
            final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
            assertThat(fof).isNull();
        }
        return null;
      }
    });
  }

  @Test
  public void rmBoundaryCases() {
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        // Should be able to handle null path
        fsh.rm(null, false);
        // Should handle non-existent paths
        fsh.rm("/doesnotexist", true);
        // Should handle non-recursive delete on folder
        fsh.rm("/", false);
        return session;
      }
    });
  }

  @Test
  public void tree() {
    final PrintWriterTester pwt = new PrintWriterTester();
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final GroupManager gm = new GroupManager(session);
        final Group group = gm.create("foo");
        final String absPath1 = "/foo/bar";
        final FileStore.Folder folder = fsh.mkdir(absPath1, true);
        folder.createFile("test.txt", "text/plain",
            new ByteArrayInputStream("Hello World!".getBytes()));
        // Assign permissions
        fm.getRoot().grantAccess(group.getPrincipal().getName(),
            FileStore.Permission.NONE);
        folder.grantAccess(group.getPrincipal().getName(),
            FileStore.Permission.RO);
        // Truncate string
        pwt.dump();
        // Check output without permissions (null & false both work)
        for (final Boolean flag : new Boolean[] { null, false }) {
          // Check tree output from root
          fsh.tree("", flag);
          assertThat(pwt.dump()).isEqualTo(
              "/ (root)\n"+
              "|-+ foo\n"+
              "|   |-+ bar\n"+
              "|   |   |-- test.txt\n");
        }
        // Check tree output with permissons
        fsh.tree("", true);
        assertThat(pwt.dump()).isEqualTo(
            "/ (root)\n"+
            "|-+ foo {foo=NONE}\n"+
            "|   |-+ bar {foo=RO}\n"+
            "|   |   |-- test.txt\n");
        // Check some things we can't do
        fsh.tree("/foo/bar/test.txt", false);
        assertThat(pwt.dump()).isEqualTo("/foo/bar/test.txt is a file\n");
        fsh.tree("/doesnotexist", false);
        assertThat(pwt.dump()).isEqualTo("no such folder /doesnotexist\n");
        return session;
      }
    }, pwt.getPrintWriter());
  }

  @Test
  public void listadmin() {
    final PrintWriterTester pwt = new PrintWriterTester();
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final GroupManager gm = new GroupManager(session);
        final Group first = gm.create("foo");
        final Group second = gm.create("bar");
        Admin.getInstance(session).getGroup().addMember(first);
        final UserDAO userDao = new UserDAO(session, jcrom());
        final User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test Example User");
        userDao.create(user);
        first.addMember(second);
        first.addMember(userDao.jackrabbitUser(user));
        // Get tree
        // TODO: Find out why we pass a null parameter!
        fsh.listadmin(null);
        // TODO: Pretty sure this output is wrong - check with Andre
        assertThat(pwt.dump()).isEqualTo(
            "|-+ filestoreAdmin\n"+
            "|-+ foo\n"+
            "|   |-+ bar\n"+
            "|   |-- test@example.com\n");
        return session;
      }
    }, pwt.getPrintWriter());
  }

  @Test
  public void importArchive() {
    final PrintWriterTester pwt = new PrintWriterTester();
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final String absPath1 = "/foo/bar";
        final FileStore.Folder folder = fsh.mkdir(absPath1, true);
        folder.createFile("test.txt", "text/plain",
            new ByteArrayInputStream("Hello World!".getBytes()));
        assertThat(fm.getFileOrFolder("/foo/bar/test.txt")).isNotNull();
        final java.io.File zipFile =
            asTempFile(fsh.createZipFile(fm.getRoot()));
        // Delete all files
        assertThat(fm.getFileOrFolder("/foo")).isNotNull();
        fm.getFileOrFolder("/foo").delete();
        assertThat(fm.getFileOrFolder("/foo/bar/test.txt")).isNull();
        // Load again from zip
        fsh.importArchive(zipFile.getAbsolutePath(), "");
        // Get file & check contents
        assertThat(fm.getFileOrFolder("/foo/bar/test.txt")).isNotNull();
        assertThat(IO.toString(((FileStore.File)
            fm.getFileOrFolder("/foo/bar/test.txt")).getData()))
            .isEqualTo("Hello World!");
        // Second load should not cause errors
        fsh.importArchive(zipFile.getAbsolutePath(), "/");
        assertThat(IO.toString(((FileStore.File)
            fm.getFileOrFolder("/foo/bar/test.txt")).getData()))
            .isEqualTo("Hello World!");
        // Load to non-root location
        fsh.importArchive(zipFile.getAbsolutePath(), "/subfolder");
        assertThat(
            fm.getFileOrFolder("/subfolder/foo/bar/test.txt")).isNotNull();
        assertThat(IO.toString(((FileStore.File)
            fm.getFileOrFolder("/subfolder/foo/bar/test.txt")).getData()))
            .isEqualTo("Hello World!");
        return session;
      }
    }, pwt.getPrintWriter());
  }

  @Test
  public void listFilesInFolder() {
    final PrintWriterTester pwt = new PrintWriterTester();
    helperTest(
        new F.Function3<Session,FileStoreHelper,FileStore.Manager,Session>() {
      @Override
      public Session apply(final Session session,
          final FileStoreHelper fsh,
          final FileStore.Manager fm) throws Throwable {
        final Iterable<List<String>> folderParts = orderedPermutations(asList(
                new String[] { "a", "b", "c", "d", "e" }));
        final SortedSet<String> folderPaths = Sets.newTreeSet();
        for (final List<String> parts : folderParts) {
          folderPaths.add("/"+Joiner.on('/').join(parts));
        }
        int fileNo = 1;
        for (final String absPath : folderPaths) {
          final FileStore.Folder folder = fsh.mkdir(absPath, true);
          for (int i = 0; i < 2; i++) {
            folder.createFile(String.format("%03d.txt", fileNo), "text/plain",
                new ByteArrayInputStream(
                    String.format("This is file %d", fileNo).getBytes()));
            fileNo++;
          }
        }
        final List<FileStore.File> files = fsh.listFilesInFolder(fm.getRoot());
        for (int i = 1; i < fileNo; i++) {
          assertThat(files.get(i-1).getName()).isEqualTo(
              String.format("%03d.txt", i));
        }
        return session;
      }
    }, pwt.getPrintWriter());
  }

  private void helperTest(final
      F.Function3<Session, FileStoreHelper, FileStore.Manager, Session> op) {
    helperTest(op, null);
  }

  private void helperTest(final
      F.Function3<Session, FileStoreHelper, FileStore.Manager, Session> op,
      final PrintWriter writer) {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(final Session session) throws Throwable {
            final FileStoreHelper fsh = writer == null ?
                new FileStoreHelper(session) :
                new FileStoreHelper(session, writer);
            op.apply(session, fsh, fileStore().getManager(session));
            return session;
          }
        });
      }
    });
  }

  private static java.io.File asTempFile(InputStream is) throws IOException {
    final java.io.File tf = File.createTempFile("zipfile", "");
    tf.deleteOnExit();
    IOUtils.copy(is, new FileOutputStream(tf));
    return tf;
  }

  private class PrintWriterTester {

    private final StringWriter sw;
    private final PrintWriter pw;

    public PrintWriterTester() {
      sw = new StringWriter();
      pw = new PrintWriter(sw);
    }

    public PrintWriter getPrintWriter() { return pw; }

    public String dump() {
      final String content = toString();
      sw.getBuffer().setLength(0);
      return content;
    }

    @Override
    public String toString() {
      pw.flush();
      return sw.getBuffer().toString();
    }

  }


}
