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
import securesocial.core.{PasswordInfo,UserId}
import service.JackrabbitSocialUserProvider
import javax.jcr.Session

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class JackrabbitUserServiceSpec extends Specification {

  "Jackrabbit Social User Provider" should {

    "allow social users to be saved" in {
      running(fakeApp) {
        val testImpl = new TestImpl(Jcr.getRepository)
        val testUser = SocialUser(
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
        testImpl.save(testUser)
        val loadedUser = testImpl.find(testUser.id).get
        loadedUser should_== testUser
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