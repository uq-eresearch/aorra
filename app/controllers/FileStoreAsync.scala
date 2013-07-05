package controllers

import java.text.DateFormat
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.jcrom.Jcrom
import com.feth.play.module.pa.user.AuthUser
import com.google.inject.Inject
import ScalaSecured.isAuthenticated
import helpers.NotificationFormatter.jsonMessage
import helpers.NotificationFormatter.sseMessage
import helpers.NotificationFormatter.ssePingMessage
import play.api.http.MimeTypes
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Input
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Accepting
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import service.filestore.EventManager.ChannelMessage
import service.filestore.EventManager.FileStoreEvent
import play.api.libs.json.JsArray

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

  import helpers.NotificationFormatter.{jsonMessage, sseMessage, ssePingMessage}

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
    val response =  JsArray(
      filestore.getEventManager().getSince(eventId) map { case (id, event) =>
        jsonMessage(id, event)
      } toSeq
    )
    Ok(response).as(JSON)
      .withHeaders("Cache-Control" -> "no-cache")
  }

  private def eventType(event: FileStoreEvent) = {
    Seq(event.info.`type`.toString(), event.`type`.toString()).mkString(":")
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
      case (id, event) => sseMessage(id, event)
    }
    Ok.feed(
        Enumerator(initialEventSourceSetup()) andThen
        pingEnumerator.interleave(
            fsEvents(authUser, lastEventId) &> eventSourceFormatter)
      ).as("text/event-stream; charset=utf-8")
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

  private def pingEnumerator(): Enumerator[String] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val dtFormat = DateFormat.getDateTimeInstance()
    Enumerator.repeatM(future {
      Thread.sleep(15000) // 15 second interval for pings
      ssePingMessage
    })
  }

  private def initialEventSourceSetup(): String = {
    "retry: 2000\n\n"
  }

}