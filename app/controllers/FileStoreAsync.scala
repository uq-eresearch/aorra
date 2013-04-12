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

/**
 *
 * The Play Framework doesn't expose something similar to `Enumerator` in Java,
 * or `Ok.feed()` with its `onComplete` and `onError` callback handling.
 *
 * As a result, notifications are implemented in Scala here.
 *
 */
class FileStoreAsync @Inject()(
      val filestore: service.filestore.FileStore)
    extends Controller with securesocial.core.SecureSocial {

  def notifications = SecuredAction { implicit request =>
    import play.api.Play.current
    val em = filestore.getEventManager
    var c: Channel[FileStoreEvent] = null;
    val e = Concurrent.unicast[FileStoreEvent](
      onStart = { channel: Channel[FileStoreEvent] =>
        c = channel
        em ! ChannelMessage.add(c)
      },
      onComplete = { () =>
        em ! ChannelMessage.remove(c)
      },
      onError = { (s: String, i: Input[FileStoreEvent]) =>
        em ! ChannelMessage.remove(c)
      }
    )
    val eventSourceFormatter = Enumeratee.map[FileStoreEvent] { event =>
      val json = Json.obj(
        "id" -> event.info.id,
        "name" -> event.info.name,
        "parentId" -> event.info.parentId,
        "type" -> event.info.`type`.toString
      )
      s"event: ${event.`type`}\ndata: $json\n\n"
    }
    Ok.feed(e &> eventSourceFormatter).as("text/event-stream")
  }

}