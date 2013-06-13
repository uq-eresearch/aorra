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
import play.api.libs.json.JsNull
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.Json
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
    if (request.accepts(MimeTypes.HTML)) {
      cometEventNotifications(authUser, request)
    } else if (request.accepts(MimeTypes.EVENT_STREAM)) {
      serverSentEventNotifications(authUser, request)
    } else {
      Status(UNSUPPORTED_MEDIA_TYPE)
    }
  }

  def cometEventNotifications(authUser: AuthUser, request: Request[AnyContent]) = {
    val cometFormatter = Enumeratee.map[(String, FileStoreEvent)] {
      case (id, event) =>
      <script>
      parent.postMessage(
        JSON.stringify({{
          'id': '{id}',
          'type': '{event.`type`}',
          'data': {scala.xml.Unparsed(event2json(event).toString())}
          }}), '*');
      </script>+"\n"
    }
    Ok.feed(
        Enumerator(initialCometSetup()) andThen
        pingEnumerator('comet).interleave(
            fsEvents(authUser) &> cometFormatter)
      ).as("text/html")
  }

  def serverSentEventNotifications(authUser: AuthUser, request: Request[AnyContent]) = {
    val lastEventId = request.headers.get("Last-Event-ID").getOrElse {
      request.queryString.get("from") match {
        case Some(Seq(a, _*)) => a.toString
        case None => null
      }
    }
    val eventSourceFormatter = Enumeratee.map[(String, FileStoreEvent)] {
      case (id, event) =>
        s"id: ${id}\nevent: ${event.`type`}\ndata: ${event2json(event)}\n\n"
    }
    Ok.feed(
        Enumerator(initialEventSourceSetup()) andThen
        pingEnumerator('sse).interleave(
            fsEvents(authUser, lastEventId) &> eventSourceFormatter)
      ).as("text/event-stream")
  }

  private def event2json(event: FileStoreEvent): JsObject = {
    Json.obj(
      "id" -> event.info.id,
      "name" -> event.info.name,
      "parentId" -> event.info.parentId,
      "attributes" -> JsObject(event.info.attributes.toSeq.map {
        case (k,v) =>
          (k, v match {
            case s: String => JsString(s)
            case n: BigDecimal => JsNumber(n)
            case _ => JsNull
          })
      }),
      "type" -> event.info.`type`.toString
    )
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

  private def initialCometSetup(): String = {
    " " * pow(2, 11).toInt + "\n" + // Pad it to kick IE into action
    // Oh, and IE8 may not know what JSON is (if in compatibility mode)
    <script src="//cdnjs.cloudflare.com/ajax/libs/json3/3.2.4/json3.min.js"></script> +
    "\n"
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