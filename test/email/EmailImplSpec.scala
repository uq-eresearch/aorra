package email

import test.AorraScalaHelper.FakeAorraApp
import test.AorraScalaHelper.asAdminUser
import test.AorraTestUtils.mailServer
import scala.util.control.Breaks._
import org.specs2.mutable.Specification
import javax.mail.internet.MimeMessage
import models.User
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class EmailImplSpec extends Specification {

  "EmailImpl" should {

    "send a HTML email" in new FakeAorraApp {
      val instance = new EmailImpl
      asAdminUser { (session, user, fakeHeaders) =>
        try {
          // Wait for the email emitted by creating the admin user account
          Await.result(mailServer().waitForEmail,
              Duration(2, TimeUnit.SECONDS))
        }
        val (_, domain) = user.getEmail.partition(_ == '@')
        val futureEmail = mailServer().waitForEmail
        instance.sendHtmlEmail(user, "Test subject", "<p>Test body</p>")
        val email = Await.result(futureEmail, Duration(2, TimeUnit.SECONDS))
        email.subject must_== "Test subject"
        email.bodyHtml must_== "<p>Test body</p>"
      }
    }

    "not send to unverified" in new FakeAorraApp {
      val instance = new EmailImpl
      asAdminUser { (session, user, fakeHeaders) =>
        try {
          // Wait for the email emitted by creating the admin user account
          Await.result(mailServer().waitForEmail,
              Duration(2, TimeUnit.SECONDS))
        }
        user.setVerified(false)
        val (_, domain) = user.getEmail.partition(_ == '@')
        val futureEmail = mailServer().waitForEmail
        instance.sendHtmlEmail(user, "Test subject", "<p>Test body</p>")
        Await.ready(futureEmail,
            Duration(2, TimeUnit.SECONDS)) must throwA[TimeoutException]
      }
    }

  }
}
