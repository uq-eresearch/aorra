package helpers;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Session;

import org.junit.Test;

import play.libs.F;
import service.filestore.FileStore;

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
        fh.mkdir("/a/1", true);
        fh.mkdir("/a/2", true);
        fh.mkdir("/b/1", true);
        ((FileStore.Folder) fm.getFileOrFolder("/a/2"))
          .createFile("test.txt", "text/plain",
              new ByteArrayInputStream(
                  "Some test content.".getBytes()));
        // Get zip file
        final java.io.File tf = fh.createZipFile(fm.getRoot());
        assertThat(tf).isNotNull();
        final ZipInputStream zis = new ZipInputStream(
            new FileInputStream(tf));
        final ZipEntry ze = zis.getNextEntry();
        assertThat(ze).isNotNull();
        assertThat(ze.getName()).isEqualTo("a/2/test.txt");
        assertThat(zis.getNextEntry()).isNull();
        zis.close();
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

  private void helperTest(final
      F.Function3<Session, FileStoreHelper, FileStore.Manager, Session> op) {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(final Session session) throws Throwable {
            op.apply(session,
                new FileStoreHelper(session),
                fileStore().getManager(session));
            return session;
          }
        });
      }
    });
  }

}
