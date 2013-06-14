package controllers

import ScalaSecured.isAuthenticated
import akka.actor.Actor
import akka.actor.Props
import com.feth.play.module.pa.PlayAuthenticate
import com.feth.play.module.pa.user.AuthUser
import com.feth.play.module.pa.user.EmailIdentity
import com.google.inject.Inject
import java.text.DateFormat
import java.util.Calendar
import javax.jcr.Credentials
import javax.jcr.Session
import models.UserDAO
import org.codehaus.jackson.node.ArrayNode
import org.jcrom.Jcrom
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Input
import play.api.libs.json.JsArray
import play.api.libs.json.JsNull
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
import play.api.mvc.Accepting
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import play.libs.F
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.future
import scala.math.pow
import service.filestore.EventManager
import service.filestore.EventManager.ChannelMessage
import service.filestore.EventManager.FileStoreEvent

/**
 *
 * The Play Framework doesn't expose something similar to `Enumerator` in Java,
 * or `Ok.feed()` with its `onComplete` and `onError` callback handling.
 *
 * As a result, notifications are implemented in Scala here.
 *
 */
class FileStoreAsync @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory)
    extends Controller {

  def notifications: EssentialAction = isAuthenticated { authUser => implicit request =>
    val AcceptsEventStream = Accepting(MimeTypes.EVENT_STREAM)
    render {
      case Accepts.Json() => pollingJsonResponse(authUser, request)
      case AcceptsEventStream() =>
        serverSentEventNotifications(authUser, request)
    }
  }

  def pollingJsonResponse(authUser: AuthUser, request: Request[AnyContent]) = {
    val eventId = lastIdInQuery(request)
    val response = JsArray(
      filestore.getEventManager().getSince(eventId) map { case (id, event) =>
        event.info match {
          case null =>
            Json.obj(
              "id" -> id,
              "type" -> event.`type`.toString()
            )
          case _ =>
            Json.obj(
              "id" -> id,
              "type" -> event.`type`.toString(),
              "data" -> event.info.id
            )
        }
      } toSeq
    )
    Ok(response).as("application/json")
      .withHeaders("Cache-Control" -> "no-cache")
  }

  private def lastIdInQuery(request: Request[AnyContent]) = {
    request.queryString.get("from") match {
      case Some(Seq(a, _*)) => a.toString
      case None => null
    }
  }

  def serverSentEventNotifications(authUser: AuthUser, request: Request[AnyContent]) = {
    val lastEventId = request.headers.get("Last-Event-ID").getOrElse {
      lastIdInQuery(request)
    }
    val eventSourceFormatter = Enumeratee.map[(String, FileStoreEvent)] {
      case (id, event) =>
        event.info match {
          case null =>
            s"id: ${id}\nevent: ${event.`type`}\ndata: ${id}\n\n"
          case _ =>
            s"id: ${id}\nevent: ${event.`type`}\ndata: ${event.info.id}\n\n"
        }
    }
    Ok.feed(
        Enumerator(initialEventSourceSetup()) andThen
        pingEnumerator('sse).interleave(
            fsEvents(authUser, lastEventId) &> eventSourceFormatter)
      ).as("text/event-stream")
  }

  private def fsEvents(authUser: AuthUser, lastEventId: String = null) = {
    val em = filestore.getEventManager
    var c: Channel[(String, FileStoreEvent)] = null;
    Concurrent.unicast[(String, FileStoreEvent)](
      onStart = { channel: Channel[(String, FileStoreEvent)] =>
        c = channel
        em tell ChannelMessage.add(c, lastEventId)
      },
      // This is a pass-by-name (ie. lazy evaluation) parameter
      // (no () => required)
      onComplete = {
        // Note: This only triggers when a new event happens and gets rejected,
        // not when the socket closes.
        em tell ChannelMessage.remove(c)
      },
      onError = { (s: String, i: Input[(String, FileStoreEvent)]) =>
        em tell ChannelMessage.remove(c)
      }
    )
  }

  private def pingEnumerator(requestType: Symbol): Enumerator[String] = {
    import ExecutionContext.Implicits.global
    val dtFormat = DateFormat.getDateTimeInstance()
    Enumerator.repeatM(future {
      Thread.sleep(15000) // 15 second interval for pings
      val msg = dtFormat.format(Calendar.getInstance().getTime())
      requestType match {
        case 'comet =>
          <script>
          parent.postMessage(
            JSON.stringify({{
              'type': 'ping',
              'data': '{msg}'
              }}), '*');
          </script>+"\n"
        case 'sse =>
          s"event: ping\ndata: ${msg}\n\n"
      }
    })
  }

  private def initialEventSourceSetup(): String = {
    "retry: 2000\n\n"
  }

  private def inUserSession[A](authUser: AuthUser, f: Session => A): A = {
    val userId = sessionFactory.inSession(new F.Function[Session, String] {
      def apply(session: Session): String = {
        authUser match {
          case a: EmailIdentity =>
            val dao = new UserDAO(session, jcrom)
            dao.findByEmail(a.getEmail()).getJackrabbitUserId
        }
      }
    });
    sessionFactory.inSession(userId, new play.libs.F.Function[Session, A] {
      def apply(session: Session): A = f(session)
    })
  }
}