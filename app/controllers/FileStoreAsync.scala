package controllers

import akka.actor.Actor
import com.google.inject.Inject
import play.api.mvc.Controller
import play.api.libs.iteratee.Input
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.concurrent.Akka
import akka.actor.Props
import service.filestore.EventManager.ChannelMessage
import service.filestore.EventManager.FileStoreEvent
import play.api.libs.iteratee.Enumeratee
import play.api.libs.json.Json
import play.api.mvc.Action
import javax.jcr.Session
import play.api.libs.iteratee.Enumerator
import com.feth.play.module.pa.PlayAuthenticate
import ScalaSecured.isAuthenticated
import com.feth.play.module.pa.user.AuthUser
import org.jcrom.Jcrom
import play.libs.F
import javax.jcr.Credentials
import com.feth.play.module.pa.user.EmailIdentity
import models.UserDAO
import scala.math.pow
import scala.concurrent.future
import scala.concurrent.ExecutionContext
import java.util.Calendar
import java.text.DateFormat
import play.api.Logger
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.http.MimeTypes
import play.api.mvc.EssentialAction
import org.codehaus.jackson.node.ArrayNode
import play.api.libs.json.JsObject
import scala.collection.JavaConversions._
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsNull

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
        Enumerator(initialCometSetup(authUser)) andThen
        pingEnumerator('comet).interleave(
            fsEvents(authUser) &> cometFormatter)
      ).as("text/html")
  }

  def serverSentEventNotifications(authUser: AuthUser, request: Request[AnyContent]) = {
    val eventSourceFormatter = Enumeratee.map[(String, FileStoreEvent)] {
      case (id, event) =>
        s"id: ${id}\nevent: ${event.`type`}\ndata: ${event2json(event)}\n\n"
    }
    Ok.feed(
        Enumerator(initialEventSourceSetup(authUser)) andThen
        pingEnumerator('sse).interleave(
            fsEvents(authUser) &> eventSourceFormatter)
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

  private def fsEvents(authUser: AuthUser) = {
    val em = filestore.getEventManager
    var c: Channel[(String, FileStoreEvent)] = null;
    Concurrent.unicast[(String, FileStoreEvent)](
      onStart = { channel: Channel[(String, FileStoreEvent)] =>
        c = channel
        em ! ChannelMessage.add(c)
      },
      // This is a pass-by-name (ie. lazy evaluation) parameter
      // (no () => required)
      onComplete = {
        // Note: This only triggers when a new event happens and gets rejected,
        // not when the socket closes.
        em ! ChannelMessage.remove(c)
      },
      onError = { (s: String, i: Input[(String, FileStoreEvent)]) =>
        em ! ChannelMessage.remove(c)
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

  private def initialCometSetup(user: AuthUser): String = {
    val jsonText = scala.xml.Unparsed(getInitialTree(user).toString())
    " " * pow(2, 11).toInt + "\n" + // Pad it to kick IE into action
    // Oh, and IE8 may not know what JSON is (if in compatibility mode)
    <script src="//cdnjs.cloudflare.com/ajax/libs/json3/3.2.4/json3.min.js"></script> +
    <script>
    parent.postMessage(
      JSON.stringify({{
        'type': 'load',
        'data': {jsonText}
      }}), '*');
    </script>+"\n"
  }

  private def initialEventSourceSetup(user: AuthUser): String = {
    Seq(
      "retry: 2000",
      s"event: load\ndata: ${getInitialTree(user)}"
    ).foldLeft("") { _ + _ + "\n\n" } // Turn into EventSource messages
  }

  private def getInitialTree(user: AuthUser): ArrayNode = {
    inUserSession(user, { (session: Session) =>
      val builder = new service.filestore.JsonBuilder(filestore, session)
      builder.tree()
    })
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