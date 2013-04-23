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


  def notifications = isAuthenticated { user => implicit request =>
    import play.api.Play.current
    val em = filestore.getEventManager
    var c: Channel[FileStoreEvent] = null;
    val e = Concurrent.unicast[FileStoreEvent](
      onStart = { channel: Channel[FileStoreEvent] =>
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
    val initialTree = inUserSession(user, { (session: Session) =>
      val builder = new service.filestore.JsonBuilder(filestore, session)
      builder.tree()
    })
    Ok.feed(
        Enumerator(s"event: load\ndata: ${initialTree}\n\n") andThen
        (e &> eventSourceFormatter)
      ).as("text/event-stream")
  }

  def inUserSession[A](authUser: AuthUser, f: Session => A): A = {
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