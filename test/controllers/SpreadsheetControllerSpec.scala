package controllers

import org.specs2.mutable.Specification
import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import play.api.test.Helpers.POST
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentType
import play.api.test.Helpers.header
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import test.AorraScalaHelper.filestore
import scala.concurrent.Await
import akka.util.Timeout
import java.util.UUID
import java.io.ByteArrayInputStream
import charts.builder.spreadsheet.XlsxDataSource
import helpers.FileStoreHelper.XLSX_MIME_TYPE
import java.io.FileInputStream

class SpreadsheetControllerSpec extends Specification {

  implicit val timeout = Timeout(30000)

  "spreadsheet external reference - check" >> {

    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val id = UUID.randomUUID.toString
        val Some(result) = route(
            FakeRequest(GET, s"/file/$id/spreadsheet-external-references"))
        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }

    "returns 404 if file doesn't exist" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val id = UUID.randomUUID.toString
        val Some(result) = route(
            FakeRequest(GET,
                s"/file/$id/spreadsheet-external-references", rh,
                AnyContentAsEmpty))
        status(result) must equalTo(404)
      }
    }

    "returns 404 if not a spreadsheet" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val file = filestore.getManager(session).getRoot()
          .createFile("test.txt", "text/plain",
            new ByteArrayInputStream("Some content".getBytes))
        val id = file.getIdentifier
        val Some(result) = route(
            FakeRequest(GET,
                s"/file/$id/spreadsheet-external-references", rh,
                AnyContentAsEmpty))
        status(result) must equalTo(404)
      }
    }

    "returns 404 if a spreadsheet w/o external refs" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val file = filestore.getManager(session).getRoot
          .createFile("test.xlsx", XLSX_MIME_TYPE,
            new FileInputStream("test/cots_outbreak.xlsx"))
        val id = file.getIdentifier
        val Some(result) = route(
            FakeRequest(GET,
                s"/file/$id/spreadsheet-external-references", rh,
                AnyContentAsEmpty))
        status(result) must equalTo(404)
      }
    }

  }


  "spreadsheet external reference - update" >> {

    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val id = UUID.randomUUID.toString
        val Some(result) = route(
            FakeRequest(POST, s"/file/$id/spreadsheet-external-references/update"))
        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }

    "returns 404 if file doesn't exist" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val id = UUID.randomUUID.toString
        val Some(result) = route(
            FakeRequest(POST,
                s"/file/$id/spreadsheet-external-references/update", rh,
                AnyContentAsEmpty))
        status(result) must equalTo(404)
      }
    }

  }

}