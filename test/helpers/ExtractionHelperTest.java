package helpers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.running;
import static test.AorraTestUtils.fakeAorraApp;
import static test.AorraTestUtils.fileStore;
import static test.AorraTestUtils.sessionFactory;
import static org.apache.tika.metadata.Office.AUTHOR;
import static org.apache.tika.metadata.Office.WORD_COUNT;

import java.io.FileInputStream;

import javax.jcr.Session;

import org.apache.tika.metadata.Metadata;
import org.junit.Test;

import play.Logger;
import play.libs.F;
import service.filestore.FileStore;

public class ExtractionHelperTest {

  @Test
  public void extractSimpleDocx() {
    running(fakeAorraApp(false), new Runnable() {
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
            // Test using interface from CRaSH scripts
            {
              final ExtractionHelper eh =
                  new ExtractionHelper(session, file.getPath());
              assertThat(eh.getPlainText())
                .startsWith("This is a test document.");
            }
            // Test using existing file object
            {
              final ExtractionHelper eh = new ExtractionHelper(file);
              assertThat(eh.getPlainText())
                .startsWith("This is a test document.");
            }
            return session;
          }
        });
      }
    });
  }

}
