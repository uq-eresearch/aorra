package helpers;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.sessionFactory;

import java.io.ByteArrayInputStream;

import javax.jcr.Session;

import org.junit.Test;

import play.libs.F;
import service.filestore.FileStore;

public class FileStoreHelperTest {

  @Test
  public void mkdirSingle() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath = "/foobar";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
              assertThat(fof).isNull();
            }
            fileStoreHelper.mkdir(absPath, false);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
              assertThat(fof).isNotNull();
              assertThat(fof).isInstanceOf(FileStore.Folder.class);
            }
            return null;
          }

        });
      }
    });
  }

  @Test
  public void mkdirSingleWhenFolderExists() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath = "/foobar";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
              assertThat(fof).isNull();
              fm.getRoot().createFolder("foobar");
            }
            try {
              fileStoreHelper.mkdir(absPath, false);
              fail("Should have thrown FolderExistsException");
            } catch (FileStoreHelper.FolderExistsException e) {
              assertThat(e.getMessage()).contains("foobar");
            }
            return null;
          }

        });
      }
    });
  }

  @Test
  public void mkdirSingleWhenFileExists() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath = "/foobar";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
              assertThat(fof).isNull();
              fm.getRoot().createFile("foobar", "text/plain",
                  new ByteArrayInputStream("Some content.".getBytes()));
            }
            try {
              fileStoreHelper.mkdir(absPath, false);
              fail("Should have thrown FileExistsException");
            } catch (FileStoreHelper.FileExistsException e) {
              assertThat(e.getMessage()).contains("foobar");
            }
            return null;
          }

        });
      }
    });
  }

  @Test
  public void mkdirMultiple() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath = "/foo/bar";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath);
              assertThat(fof).isNull();
            }
            try {
              fileStoreHelper.mkdir(absPath, false);
              fail("Should have thrown FolderNotFoundException");
            } catch (FileStoreHelper.FolderNotFoundException e) {
              assertThat(e.getMessage()).contains("foo");
            }
            return null;
          }

        });
      }
    });
  }

  @Test
  public void mkdirMultipleWithParentCreation() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath1 = "/foo/bar";
            final String absPath2 = "/foo/baz";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
              assertThat(fof).isNull();
            }
            fileStoreHelper.mkdir(absPath1, true);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
              assertThat(fof).isNotNull();
              assertThat(fof).isInstanceOf(FileStore.Folder.class);
            }
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath2);
              assertThat(fof).isNull();
            }
            fileStoreHelper.mkdir(absPath2, true);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath2);
              assertThat(fof).isNotNull();
              assertThat(fof).isInstanceOf(FileStore.Folder.class);
            }
            return null;
          }

        });
      }
    });
  }
  

  @Test
  public void rmFolder() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileStoreHelper fileStoreHelper =
                new FileStoreHelper(session);
            final String absPath1 = "/foo/bar";
            FileStore.Manager fm = fileStore().getManager(session);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
              assertThat(fof).isNull();
            }
            fileStoreHelper.mkdir(absPath1, true);
            {
              final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
              assertThat(fof).isNotNull();
              assertThat(fof).isInstanceOf(FileStore.Folder.class);
            }
            fileStoreHelper.rm(absPath1, true);
            {
                final FileStore.FileOrFolder fof = fm.getFileOrFolder(absPath1);
                assertThat(fof).isNull();
            }
            return null;
          }
        });
      }
    });
  }

}
