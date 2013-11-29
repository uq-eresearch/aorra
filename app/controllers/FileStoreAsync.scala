package controllers

import java.text.DateFormat

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future

import org.jcrom.Jcrom

import com.feth.play.module.pa.user.AuthUser
import com.google.inject.Inject

import ScalaSecured.isAuthenticated
import helpers.EventFormatter.jsonMessage
import helpers.EventFormatter.sseMessage
import helpers.EventFormatter.ssePingMessage
import play.api.http.MimeTypes
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Input
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Accepting
import play.api.mvc.AnyContent
import play.api.mvc.Controller
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import service.EventManager.EventReceiver;
import service.EventManager.EventReceiverMessage;
import service.EventManager.{Event => EmEvent}
import play.api.libs.json.JsArray
import service.OrderedEvent
import models.CacheableUser

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

  import helpers.EventFormatter.{jsonMessage, sseMessage, ssePingMessage}

  val ssePingBroadcast: Enumerator[String] = {
    import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
    val (enumerator, channel) = Concurrent.broadcast[String]
    // Ping every 15 seconds, after the first 1 second delay
    Akka.system.scheduler.schedule(1.seconds, 15.seconds) {
      channel.push(ssePingMessage)
    }
    // Shed
    enumerator
  }

  def events: EssentialAction = isAuthenticated { user => implicit request =>
    val AcceptsEventStream = Accepting(MimeTypes.EVENT_STREAM)
    render {
      case Accepts.Json() => pollingJsonResponse(request)
      case AcceptsEventStream() => serverSentEvents(request)
    }
  }

  def pollingJsonResponse(request: Request[AnyContent]) = {
    val response = JsArray(
      filestore.getEventManager().getSince(lastId(request)).map {
        case OrderedEvent(id, event) => jsonMessage(id, event)
      }.toSeq
    )
    Ok(response).as(JSON)
      .withHeaders("Cache-Control" -> "max-age=0, must-revalidate")
  }

  private def lastId(request: Request[AnyContent]) =
    request.headers.get("Last-Event-ID").getOrElse {
      request.queryString.get("from") match {
        case Some(Seq(a, _*)) => a.toString
        case None => null
      }
    }

  def serverSentEvents(request: Request[AnyContent]) =
    Ok.feed(
        initialEventSourceSetup andThen
        (fsEvents(lastId(request)) &> eventSourceFormatter).interleave(
            pingEnumerator)
      ).as("text/event-stream; charset=utf-8")
       .withHeaders("Cache-Control" -> "max-age=0, must-revalidate")

  private def fsEvents(lastEventId: String) = {
    val em = filestore.getEventManager
    var er: EventReceiver = null;
    Concurrent.unicast[OrderedEvent](
      onStart = { channel: Channel[OrderedEvent] =>
        er = ChannelEventReceiver(channel)
        em tell EventReceiverMessage.add(er, lastEventId)
      },
      // This is a pass-by-name (ie. lazy evaluation) parameter
      // (no () => required)
      onComplete = {
        // Note: This only triggers when a new event happens and gets rejected,
        // not when the socket closes.
        em tell EventReceiverMessage.remove(er)
      },
      onError = { (s: String, i: Input[OrderedEvent]) =>
        em tell EventReceiverMessage.remove(er)
      }
    )
  }

  private def pingEnumerator(): Enumerator[String] = ssePingBroadcast

  private def initialEventSourceSetup = Enumerator("retry: 2000\n\n")

  private def eventSourceFormatter = {
    // Play Framework uses structural types to implement the Enumeratee
import scala.language.reflectiveCalls
    Enumeratee.map[OrderedEvent] {
      case OrderedEvent(id, event) => sseMessage(id, event)
    }
  }

  private case class ChannelEventReceiver(val c: Channel[OrderedEvent])
      extends EventReceiver {
    def push(oe: OrderedEvent) = c.push(oe)
    def end = c.end
    def end(e: Throwable) = c.end(e)
  }

}