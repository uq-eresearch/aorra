package helpers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.sessionFactory;

import javax.jcr.Session;

import play.libs.F;
import service.filestore.FileStore;

public class FileStoreHelperTest {

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
            fileStoreHelper.mkdir(absPath, true);
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

}
