package controllers

import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

import scala.Array.canBuildFrom
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.specs2.mutable.Specification

import akka.util.Timeout
import helpers.FileStoreHelper.XLSX_MIME_TYPE
import helpers.FileStoreHelper.XLS_MIME_TYPE
import javax.jcr.Session
import models.User
import play.api.libs.iteratee.Iteratee
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results
import play.api.mvc.SimpleResult
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentType
import play.api.test.Helpers.header
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import test.AorraScalaHelper.filestore

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
        val file = folder.createFile("marine.xlsx", XLSX_MIME_TYPE,
            new FileInputStream("test/marine.xlsx"))

        val Some(futureResult: Future[SimpleResult]) = route(FakeRequest(GET,
            routes.ArchiveAsync.chartArchive(file.getIdentifier).toString,
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

        // Wait 2 minutes for all bytes to be collected
        val zipBytes: Array[Byte] =
          Await.result(futureZipBytes, Duration(2, TimeUnit.MINUTES))

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
              folderId must equalTo(file.getName())
              filename
          }
        }.groupBy { filename =>
          filename.split("\\.").last
        }

        filenamesByExt must haveKeys("csv", "emf", "pdf", "png", "svg")

        filenamesByExt.forall { case (k, v) =>
          v must haveSize(7)
        }
      }
    }

    "handle charts with extra parameters" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val folder = filestore.getManager(session).getRoot()
        val file = folder.createFile("seagrass_cover.xls", XLS_MIME_TYPE,
            new FileInputStream("test/seagrass_cover.xls"))

        val Some(futureResult: Future[SimpleResult]) = route(FakeRequest(GET,
            routes.ArchiveAsync.chartArchive(file.getIdentifier).toString,
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

        // Wait 2 minutes for all bytes to be collected
        val zipBytes: Array[Byte] =
          Await.result(futureZipBytes, Duration(2, TimeUnit.MINUTES))

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
              folderId must equalTo(file.getName())
              filename
          }
        }.groupBy { filename =>
          filename.split("\\.").last
        }

        filenamesByExt must haveKeys("emf", "pdf", "png", "svg")

        filenamesByExt.forall { case (k, v) =>
          v must haveSize(15)
        }
      }
    }

  }

}