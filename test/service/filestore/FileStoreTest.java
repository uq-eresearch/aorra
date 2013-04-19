package service.filestore;

import static org.fest.assertions.Assertions.*;
import static play.test.Helpers.running;

import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import static test.AorraTestUtils.fakeAorraApp;

import org.junit.Test;

import play.Play;
import play.libs.F.Function;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;

public class FileStoreTest {
  @Test
  public void canGetFolders() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        final JcrSessionFactory sessionFactory = GuiceInjectionPlugin
            .getInjector(Play.application())
            .getInstance(JcrSessionFactory.class);
        final FileStore fileStore = new FileStore(sessionFactory);
        sessionFactory.inSession(new Function<Session,FileStore.Folder>() {
          @Override
          public FileStore.Folder apply(Session session) {
            try {
              Set<FileStore.Folder> folders =
                  fileStore.getManager(session).getFolders();
              assertThat(folders).hasSize(1);
              FileStore.Folder folder = folders.iterator().next();
              assertThat(folder.getPath()).isEqualTo("/");
              return folder;
            } catch (RepositoryException e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    });
  }

}
