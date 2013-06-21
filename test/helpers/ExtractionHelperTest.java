package helpers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.sessionFactory;
import static org.apache.tika.metadata.Office.AUTHOR;
import static org.apache.tika.metadata.Office.PAGE_COUNT;

import java.io.FileInputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.tika.metadata.Metadata;
import org.junit.Test;

import play.libs.F;
import service.filestore.FileStore;

public class ExtractionHelperTest {

  @Test
  public void extractSimpleDocx() {
    running(fakeAorraApp(), new Runnable() {
      @Override
      public void run() {
        sessionFactory().inSession(new F.Function<Session,Session>() {
          @Override
          public Session apply(Session session) throws Throwable {
            final FileInputStream data = new FileInputStream("test/test.docx");
            final FileStore.Manager fm = fileStore().getManager(session);
            final FileStore.File file = fm.getRoot().createFile(
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                data);
            // Test using existing file object
            {
              final ExtractionHelper eh = new ExtractionHelper(file);
              testPlainText(eh);
              testMetadata(eh);
            }
            // Test using interface from CRaSH scripts
            {
              final ExtractionHelper eh =
                  new ExtractionHelper(session, file.getPath());
              testPlainText(eh);
              testMetadata(eh);
            }
            // Test using interface from CRaSH scripts with version
            {
              final ExtractionHelper eh =
                  new ExtractionHelper(session, file.getPath(), "1.0");
              testPlainText(eh);
              testMetadata(eh);
            }
            {
              final ExtractionHelper eh =
                  new ExtractionHelper(session, file.getPath(), null);
              testPlainText(eh);
              testMetadata(eh);
            }
            return session;
          }
        });
      }

      private void testPlainText(ExtractionHelper eh)
          throws RepositoryException {
        assertThat(eh.getPlainText()).startsWith("This is a test document.");
      }

      private void testMetadata(ExtractionHelper eh)
          throws RepositoryException {
        final Metadata metadata = eh.getMetadata();
        assertThat(metadata.get(AUTHOR)).isEqualTo("Tim Dettrick");
        assertThat(metadata.get(PAGE_COUNT)).isEqualTo("1");
      }

    });
  }

}
