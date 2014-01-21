package test

import com.typesafe.plugin.MailerPlugin
import com.typesafe.plugin.MailerAPI
import com.typesafe.plugin.MailerBuilder
import play.api.Application
import scala.concurrent.Promise
import scala.concurrent.Future
import scala.collection.mutable.SynchronizedQueue

class FakeMailPlugin(application: Application) extends MailerPlugin {

  case class MockEmail(
      val headers: Map[String, List[String]],
      val subject: String, val bodyText: String, val bodyHtml: String) {}

  private val waiting = new SynchronizedQueue[Promise[MockEmail]]()

  override lazy val email: MailerAPI = TestMailer({ email: MockEmail =>
    waiting.dequeueAll(_ => true).foreach(_.success(email))
  })

  def waitForEmail: Future[MockEmail] = {
    val p = Promise[MockEmail]()
    waiting += p
    p.future
  }

  case class TestMailer(addToQueue: MockEmail => Unit) extends MailerBuilder {
    def send(bodyText: String, bodyHtml: String): Unit =
      addToQueue(MockEmail(
        Map("from" -> e("from"),
            "replyTo" -> e("replyTo"),
            "recipients" -> e("recipients"),
            "ccRecipients" -> e("ccRecipients"),
            "bccRecipients" -> e("bccRecipients")),
        e("subject").head,
        bodyText,
        bodyHtml))
  }

}