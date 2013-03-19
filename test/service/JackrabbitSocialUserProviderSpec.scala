package test.service

import test.AorraTestUtils.fakeApp
import com.wingnest.play2.jackrabbit.Jcr
import java.io.File
import javax.jcr.Repository
import org.apache.commons.io.FileUtils
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.libs.oauth.OAuth
import play.api.test.Helpers._
import play.api.test._
import securesocial.core.AuthenticationMethod
import securesocial.core.SocialUser
import securesocial.core.{Identity,PasswordInfo,UserId}
import service.JackrabbitSocialUserProvider
import javax.jcr.Session
import models.User
import java.util.UUID

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class JackrabbitUserServiceSpec extends Specification {

  "Jackrabbit Social User Provider" should {

    val testSocialUser = SocialUser(
      UserId("testuser", "testprovider"),
      "Test", "User", "Test User",
      Some("test@example.com"),
      Some("http://example.test/"),
      AuthenticationMethod.UserPassword,
      None,
      None,
      Some(PasswordInfo(
        "bcrypt",
        "$2a$10$IK84/N39CQ.zdYgmc8I3o.XOMYm1SoJobtI35XsVa5MgI1/MaMzhS",
        None)))


    "allow social users to be saved" in {
      running(fakeApp) {
        val testImpl = new TestImpl(Jcr.getRepository)
        val testUser = testSocialUser
        testImpl.save(testUser)
        val loadedUser = testImpl.find(testUser.id).get

        // OK, let's run through Identity and check all the methods match
        loadedUser.id should_== testUser.id
        loadedUser.firstName should_== testUser.firstName
        loadedUser.lastName should_== testUser.lastName
        loadedUser.fullName should_== testUser.fullName
        loadedUser.email should_== testUser.email
        loadedUser.avatarUrl should_== testUser.avatarUrl
        loadedUser.authMethod should_== testUser.authMethod
        loadedUser.oAuth1Info should_== testUser.oAuth1Info
        loadedUser.oAuth2Info should_== testUser.oAuth2Info
        loadedUser.passwordInfo should_== testUser.passwordInfo
      }
    }

    "use UUIDs for usernames" in {
      running(fakeApp) {
        val testImpl = new TestImpl(Jcr.getRepository)
        val loadedUser = testImpl.save(testSocialUser)

        // It should be a User
        val u = loadedUser.asInstanceOf[User]

        val uuidRegex =
          """[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}""".r
        uuidRegex.findFirstIn(u.jackrabbitUser.getID) must beSome
      }
    }

    "create users that can be impersonated" in {
      running(fakeApp) {
        val testImpl = new TestImpl(Jcr.getRepository)
        val loadedUser = testImpl.save(testSocialUser)

        // It should be a User
        val u = loadedUser.asInstanceOf[User]
        testImpl.inSession { session =>
          val newSession = u.impersonate(session)
          newSession.getUserID() should_== u.jackrabbitUser.getID
          newSession.logout()
          session.isLive must beTrue
        }
      }
    }
  }

  class TestImpl(repository: Repository) extends JackrabbitSocialUserProvider {
    override def inSession[A](op: (Session) => A): A = {
      val session = Jcr.login("admin", "admin")
      op(session)
      // Note: We don't close the session, because it constantly gets reused.
    }
  }

}