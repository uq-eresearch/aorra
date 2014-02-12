package controllers

import java.io.ByteArrayInputStream
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.specs2.mutable.Specification
import javax.jcr.Session
import models.User
import play.api.mvc.AnyContentAsEmpty
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
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import akka.util.Timeout

class FileStoreAsyncSpec extends Specification {

  implicit val timeout = Timeout(30000)

  type RT = AnyContentAsEmpty.type

  "notifications" should {

    "requires login" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(FakeRequest(GET, "/events"))

        status(result) must equalTo(303)
        header("Location", result) must beSome("/login")
      }
    }

    "returns JSON by default" in new FakeAorraApp {
      asAdminUser { (session: Session, user: User, rh: FakeHeaders) =>
        val Some(result) = route(
            FakeRequest(GET, "/events", rh, AnyContentAsEmpty))

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
        def testOutOfDate(): String = {
          val Some(result) = route(
              FakeRequest(GET, "/events", rh, AnyContentAsEmpty)
                .withHeaders( "Accept" -> "text/event-stream" ))

          status(result) must equalTo(OK);
          contentType(result) must beSome("text/event-stream")
          charset(result) must beSome("utf-8")
          header("Cache-Control", result) must
            beSome("max-age=0, must-revalidate");

          val simpleResult = Await.result(result,
              Duration(30, TimeUnit.SECONDS))

          val consume = Iteratee.getChunks[Array[Byte]]
          val takeN = Enumeratee.take[Array[Byte]](3)
          val eventualResult: List[String] = Await.result(
            Iteratee.flatten(simpleResult.body through takeN apply consume).run,
            Duration(30, TimeUnit.SECONDS)
          ).map(new String(_))

          eventualResult must contain("retry: 2000\n\n")
          val outOfDateMsg = eventualResult find ( _.contains("outofdate") )
          outOfDateMsg must not beNone
          val IdPattern = "(?s)id: ([0-9a-f]+)\n.*".r
          return outOfDateMsg match {
            case Some(IdPattern(id)) => id.toString
            case _ => null
          }
        }
        def testFolderCreate(id: String) {
          val Some(result) = route(
              FakeRequest(GET, "/events", rh, AnyContentAsEmpty)
                .withHeaders(
                    "Accept" -> "text/event-stream",
                    "Last-Event-ID" -> id
                ))

          status(result) must equalTo(OK);
          contentType(result) must beSome("text/event-stream")
          charset(result) must beSome("utf-8")
          header("Cache-Control", result) must
            beSome("max-age=0, must-revalidate");

          val folder = filestore.getManager(session).getRoot()
              .createFolder("Test Folder")

          val simpleResult = Await.result(result,
              Duration(30, TimeUnit.SECONDS))

          val consume = Iteratee.getChunks[Array[Byte]]
          val takeN = Enumeratee.take[Array[Byte]](2)
          val eventualResult: List[String] = Await.result(
            Iteratee.flatten(simpleResult.body through takeN apply consume).run,
            Duration(30, TimeUnit.SECONDS)
          ).map(new String(_))

          eventualResult must contain("retry: 2000\n\n")
          val createMsg = eventualResult find ( _.contains("folder:create") )
          createMsg must not beNone
          val DataPattern = "(?s).*\ndata: ([0-9a-f\\-]+)\n\n".r
          createMsg match {
            case Some(DataPattern(data)) => data must_== folder.getIdentifier()
            case _ => failure("Folder message should contain data.")
          }
        }

        val lastId = testOutOfDate()
        if (lastId == null) {
          failure("Out-of-date message should contain ID.")
        } else {
        // Try again with ID
          testFolderCreate(lastId)
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
        val expectedId = events.head.id

        val Some(result) = route(
            FakeRequest(GET, "/events",
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