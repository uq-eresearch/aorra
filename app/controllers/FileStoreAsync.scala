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
import service.filestore.EventManager.ChannelMessage
import service.filestore.EventManager.{Event => EmEvent}
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

  def events: EssentialAction = isAuthenticated { authUser => implicit request =>
    val AcceptsEventStream = Accepting(MimeTypes.EVENT_STREAM)
    render {
      case Accepts.Json() => pollingJsonResponse(authUser, request)
      case AcceptsEventStream() => serverSentEvents(authUser, request)
    }
  }

  def pollingJsonResponse(authUser: AuthUser, request: Request[AnyContent]) = {
    val eventId = lastIdInQuery(request)
    val response =  JsArray(
      filestore.getEventManager().getSince(eventId).map { case (id, event) =>
        jsonMessage(id, event)
      }.toSeq
    )
    Ok(response).as(JSON)
      .withHeaders("Cache-Control" -> "max-age=0, must-revalidate")
  }

  private def lastIdInQuery(request: Request[AnyContent]) = {
    request.queryString.get("from") match {
      case Some(Seq(a, _*)) => a.toString
      case None => null
    }
  }

  def serverSentEvents(authUser: AuthUser, request: Request[AnyContent]) = {
    val lastEventId = request.headers.get("Last-Event-ID").getOrElse {
      lastIdInQuery(request)
    }
    Ok.feed(
        initialEventSourceSetup andThen
        (fsEvents(authUser, lastEventId) &> eventSourceFormatter).interleave(
            pingEnumerator)
      ).as("text/event-stream; charset=utf-8")
       .withHeaders("Cache-Control" -> "max-age=0, must-revalidate")
  }

  private def fsEvents(authUser: AuthUser, lastEventId: String) = {
    val em = filestore.getEventManager
    var c: Channel[(String, EmEvent)] = null;
    Concurrent.unicast[(String, EmEvent)](
      onStart = { channel: Channel[(String, EmEvent)] =>
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
      onError = { (s: String, i: Input[(String, EmEvent)]) =>
        em tell ChannelMessage.remove(c)
      }
    )
  }

  private def pingEnumerator(): Enumerator[String] = ssePingBroadcast

  private def initialEventSourceSetup(): Enumerator[String] = {
    Enumerator("retry: 2000\n\n")
  }

  private def eventSourceFormatter() = {
    // Play Framework uses structural types to implement the Enumeratee
    import scala.language.reflectiveCalls
    Enumeratee.map[(String, EmEvent)] {
      case (id, event) => sseMessage(id, event)
    }
  }

}