package controllers

import java.io.ByteArrayInputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.specs2.mutable.Specification
import javax.jcr.Session
import models.User
import scala.concurrent.Await
import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AsyncResult
import play.api.mvc.ChunkedResult
import play.api.mvc.Results
import play.api.mvc.SimpleResult
import play.api.mvc.Results.EmptyContent
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.test.Helpers.OK
import play.api.test.Helpers.charset
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.contentType
import play.api.test.Helpers.header
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import test.AorraScalaHelper.filestore
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import play.api.libs.iteratee.{Enumeratee, Iteratee}
import scala.concurrent.Future
import java.util.zip.{ZipInputStream, ZipEntry}
import helpers.FileStoreHelper.{XLS_MIME_TYPE, XLSX_MIME_TYPE}
import java.io.FileInputStream
import scala.collection.immutable.StringOps
import akka.util.Timeout

class ArchiveAsyncSpec extends Specification {

  implicit val timeout = Timeout(30000)

  "chart archive download" should {

    "require login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val folder = filestore.getManager(session).getRoot()

        val Some(result) = route(FakeRequest(GET,
            routes.ArchiveAsync.chartArchive(folder.getIdentifier).toString))

        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }

    "return archive" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        import scala.concurrent.ExecutionContext.Implicits.global

        val folder = filestore.getManager(session).getRoot()
        folder.createFile("marine.xlsx", XLSX_MIME_TYPE,
            new FileInputStream("test/marine.xlsx"))

        val Some(futureResult: Future[SimpleResult]) = route(FakeRequest(GET,
            routes.ArchiveAsync.chartArchive(folder.getIdentifier).toString,
            rh, AnyContentAsEmpty))

        status(futureResult) must equalTo(OK);
        contentType(futureResult) must beSome("application/zip")
        header("Cache-Control", futureResult) must
          beSome("max-age=0, must-revalidate");

        // Need to work with actual result
        val result = Await.result(futureResult, Duration(30, TimeUnit.SECONDS))

        // Take "Transfer-Encoding: chunked" bytes, dechunk and collect
        val futureZipBytes = result.body.through(Results.dechunk)
            .run(Iteratee.consume[Array[Byte]]())

        // Wait for 30 seconds for all bytes to be collected
        val zipBytes: Array[Byte] =
          Await.result(futureZipBytes, Duration(30, TimeUnit.SECONDS))

        // Check magic number of sent file
        zipBytes.slice(0, 2) must equalTo("PK".getBytes)

        val filepaths: Set[String] = {
          val zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))
          Stream.continually(zis.getNextEntry)
            .takeWhile(_ != null)
            .map(_.getName)
            .toSet
        }

        val filenamesByExt = filepaths.map { filepath =>
          filepath.split("/") match {
            case Array(folderId, filename) =>
              folderId must equalTo(folder.getIdentifier())
              filename
          }
        }.groupBy { filename =>
          filename.split("\\.").last
        }

        filenamesByExt must haveKeys("csv", "docx", "emf", "html", "png", "svg")

        filenamesByExt.forall { case (k, v) =>
          v must haveSize(7)
        }
      }
    }

    "handle charts with extra parameters" in new FakeAorraApp {
        asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val folder = filestore.getManager(session).getRoot()
        folder.createFile("seagrass_cover.xls", XLS_MIME_TYPE,
            new FileInputStream("test/seagrass_cover.xls"))

        val Some(futureResult: Future[SimpleResult]) = route(FakeRequest(GET,
            routes.ArchiveAsync.chartArchive(folder.getIdentifier).toString,
            rh, AnyContentAsEmpty))

        status(futureResult) must equalTo(OK);
        contentType(futureResult) must beSome("application/zip")
        header("Cache-Control", futureResult) must
          beSome("max-age=0, must-revalidate");

        // Need to work with actual result
        val result = Await.result(futureResult, Duration(30, TimeUnit.SECONDS))

        // Take "Transfer-Encoding: chunked" bytes, dechunk and collect
        val futureZipBytes = result.body.through(Results.dechunk)
            .run(Iteratee.consume[Array[Byte]]())

        // Wait for 30 seconds for all bytes to be collected
        val zipBytes: Array[Byte] =
          Await.result(futureZipBytes, Duration(30, TimeUnit.SECONDS))

        // Check magic number of sent file
        zipBytes.slice(0, 2) must equalTo("PK".getBytes)

        val filepaths: Set[String] = {
          val zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))
          Stream.continually(zis.getNextEntry)
            .takeWhile(_ != null)
            .map(_.getName)
            .toSet
        }

        val filenamesByExt = filepaths.map { filepath =>
          filepath.split("/") match {
            case Array(folderId, filename) =>
              folderId must equalTo(folder.getIdentifier())
              filename
          }
        }.groupBy { filename =>
          filename.split("\\.").last
        }

        filenamesByExt must haveKeys("docx", "emf", "png", "svg")

        filenamesByExt.forall { case (k, v) =>
          v must haveSize(15)
        }
      }

    }

  }

}