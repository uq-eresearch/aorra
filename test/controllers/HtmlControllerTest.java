package controllers;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentType;
import static play.test.Helpers.header;
import static play.test.Helpers.status;
import static test.AorraTestUtils.asAdminUser;
import static test.AorraTestUtils.fileStore;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Session;

import models.User;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import play.libs.F;
import play.mvc.Result;
import play.test.FakeRequest;
import service.filestore.FileStore;

public class HtmlControllerTest {

    @Test
    public void htmlzip() {
      asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
        @Override
        public Session apply(
            final Session session,
            final User user,
            final FakeRequest newRequest) throws Throwable {
          final FileStore.Manager fsm = fileStore().getManager(session);
          FileStore.Folder root = fsm.getRoot();
          FileStore.File htmlFile = root.createFile("foo.html", "text/html", new ByteArrayInputStream((
            "<html><body>some html content</body></html>").getBytes()));
          final Result result = callAction(
                  controllers.routes.ref.HtmlController.toHtmlZip(htmlFile.getIdentifier()),
                  newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/zip");
          assertThat(header("Content-Disposition", result))
            .startsWith("attachment; filename=foo.html.zip");
          byte[] zip = play.test.Helpers.contentAsBytes(result);
          assertThat(containsFile(zip, "foo.html")).isTrue();
          return session;
        }
      });
    }

    @Test
    public void pdf() {
      asAdminUser(new F.Function3<Session, User, FakeRequest, Session>() {
        @Override
        public Session apply(
            final Session session,
            final User user,
            final FakeRequest newRequest) throws Throwable {
          final FileStore.Manager fsm = fileStore().getManager(session);
          FileStore.Folder root = fsm.getRoot();
          FileStore.File htmlFile = root.createFile("foo.html", "text/html", new ByteArrayInputStream((
            "<html><body>some html content</body></html>").getBytes()));
          final Result result = callAction(
                  controllers.routes.ref.HtmlController.toPdf(htmlFile.getIdentifier(), "", ""),
                  newRequest);
          assertThat(status(result)).isEqualTo(200);
          assertThat(contentType(result)).isEqualTo("application/pdf");
          assertThat(header("Content-Disposition", result))
            .startsWith("attachment; filename=foo.html.pdf");
          byte[] pdf = play.test.Helpers.contentAsBytes(result);
          assertThat(Hex.encodeHexString(ArrayUtils.subarray(pdf, 0, 4))).isEqualTo("25504446");
          return session;
        }
      });
    }

    private boolean containsFile(byte[] zip, String file) throws Exception {
        ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(zip));
        while(true) {
            ZipEntry entry = z.getNextEntry();
            if(entry == null) {
                break;
            }
            if(StringUtils.equals(entry.getName(), file)) {
                return true;
            }
            z.closeEntry();
        }
        z.close();
        return false;
    }
}
