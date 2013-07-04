package controllers

import org.specs2.mutable._
import FileStoreControllerTest.{asAdminUser => jAsAdminUser}
import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.libs.F
import play.test.{FakeRequest => JFakeRequest}
import org.specs2.matcher.MatchFailureException
import org.specs2.execute.PendingException
import play.api.mvc.WithHeaders
import play.api.mvc.AsyncResult
import test.AorraScalaHelper._
import test.AorraTestUtils
import java.io.ByteArrayInputStream
import scala.collection.JavaConversions._

class FileStoreAsyncSpec extends Specification {

  type RT = AnyContentAsEmpty.type

  "notifications" should {

    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(FakeRequest(GET, "/notifications"))

        status(result) must equalTo(303)
        header("Location", result) must beSome("/")
      }
    }

    "returns JSON by default" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(
            FakeRequest(GET, "/notifications", rh, AnyContentAsEmpty))

        status(result) must equalTo(OK);
        contentType(result) must beSome("application/json")
        charset(result) must beSome("utf-8")
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

        contentAsString(result) must contain(expectedId)
        contentAsString(result) must contain("file:create")
        contentAsString(result) must contain(file.getIdentifier)
      }
    }

  }

  // Convert asAdminUser to work well with Scala
  private def asAdminUser(f: (Session, User, FakeHeaders) => Unit) = {
    try {
      jAsAdminUser(new F.Function3[Session, User, JFakeRequest, Session]() {
        def apply(session: Session, u: User, jfr: JFakeRequest) = {
          val headers = jfr.getWrappedRequest().headers match {
            case fakeHeaders: FakeHeaders => fakeHeaders
          }
          f(session, u, headers)
          session
        }
      });
    } catch {
      // Catch and rethrow Specs2 exceptions
      case e: RuntimeException =>
        e.getCause() match {
          case pe: PendingException => throw pe
          case mfe: MatchFailureException[_] => throw mfe
          case _ => throw e
        }
    }
  }

}