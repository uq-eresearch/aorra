package controllers

import java.io.ByteArrayInputStream

import scala.collection.JavaConversions.iterableAsScalaIterable

import org.specs2.mutable.Specification

import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.AsyncResult
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

class FileStoreAsyncSpec extends Specification {

  type RT = AnyContentAsEmpty.type

  "notifications" should {

    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(FakeRequest(GET, "/notifications"))

        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }

    "returns JSON by default" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(
            FakeRequest(GET, "/notifications", rh, AnyContentAsEmpty))

        status(result) must equalTo(OK);
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
        header("Cache-Control", result) must
          beSome("max-age=0, must-revalidate");
        contentAsString(result) must equalTo("[]")
      }
    }

    "returns Server Sent Events based on Accept header" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(
            FakeRequest(GET, "/notifications", rh, AnyContentAsEmpty)
              .withHeaders( "Accept" -> "text/event-stream" ))

        status(result) must equalTo(OK);
        contentType(result) must beSome("text/event-stream")
        charset(result) must beSome("utf-8")
        header("Cache-Control", result) must
          beSome("max-age=0, must-revalidate");

        result match {
          case AsyncResult(_) => // all good
          case _ => failure("Should have an asynchronous result.")
        }

      }
    }

    "returns events in the past" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val file = filestore.getManager(session).getRoot()
          .createFile("test.txt", "text/plain",
            new ByteArrayInputStream("Some content".getBytes))

        // Check event is there
        val events = filestore.getEventManager().getSince(null)
          .toIterable.toSeq
        events must haveSize(1)
        val expectedId = events.head._1

        val Some(result) = route(
            FakeRequest(GET, "/notifications",
                rh, AnyContentAsEmpty))

        status(result) must equalTo(OK);
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
        header("Cache-Control", result) must
          beSome("max-age=0, must-revalidate");

        contentAsString(result) must contain(expectedId)
        contentAsString(result) must contain("file:create")
        contentAsString(result) must contain(file.getIdentifier)
      }
    }

  }

}