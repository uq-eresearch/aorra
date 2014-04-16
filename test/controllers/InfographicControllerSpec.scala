package controllers

import org.specs2.mutable.Specification
import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, HEAD, POST}
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentType
import play.api.test.Helpers.header
import play.api.test.Helpers.route
import play.api.test.Helpers.status
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import scala.concurrent.Await
import akka.util.Timeout
import java.util.UUID
import scala.io.Source

class InfographicControllerSpec extends Specification {

  implicit val timeout = Timeout(30000)

  "infographic data.json" >> {
    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val id = UUID.randomUUID.toString
        val Some(result) = route(
            FakeRequest(GET, s"/file/$id/infographic/data.json"))
        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }
  }

  "infographic static files" >> {
    "should exist for every entry in manifest.txt" in new FakeAorraApp {
      val manifest = Source.fromInputStream(
        getClass.getResourceAsStream("/infographic/manifest.txt")).getLines
      val id = UUID.randomUUID.toString
      manifest.foreach { staticFile =>
        val Some(result) = route(
            FakeRequest(GET, s"/file/$id/infographic/$staticFile"))
        status(result) must equalTo(200).setMessage(s"$staticFile should exist")
      }
    }
  }

}