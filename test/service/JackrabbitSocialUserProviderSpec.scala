package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import javax.jcr.Repository
import org.apache.jackrabbit.core.TransientRepository
import javax.jcr.Credentials
import javax.jcr.RepositoryException
import org.apache.commons.io.FileUtils
import org.specs2.specification.Scope
import java.io.File
import service.JackrabbitSocialUserProvider
import javax.jcr.SimpleCredentials
import org.apache.jackrabbit.api.JackrabbitSession
import securesocial.core.SocialUser
import securesocial.core.AuthenticationMethod
import play.api.libs.oauth.OAuth
import securesocial.core.{PasswordInfo,UserId}

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class JackrabbitUserServiceSpec extends Specification {

  "Jackrabbit Social User Provider" should {

    "allow social users to be saved" in new CleanJackrabbitEnvironment {
      val testImpl = new TestImpl(repository)
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

  trait CleanJackrabbitEnvironment extends Scope with BeforeAfter {
    import FileUtils.deleteDirectory

    val REPOSITORY_CONFIG_PATH = "test/repository.xml"
    val REPOSITORY_DIRECTORY_PATH = "./target/jackrabbittestrepository"

    lazy val repository = new TransientRepository(
      REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH)

    def before = if (repositoryDir.exists()) { deleteDirectory(repositoryDir) }
    def after = {
      repositoryDir.exists.should_==(true)
      deleteDirectory(repositoryDir)
      repositoryDir.exists.should_==(false)
    }
    private def repositoryDir = new File(REPOSITORY_DIRECTORY_PATH)
  }

  class TestImpl(repository: Repository) extends JackrabbitSocialUserProvider {

    lazy val session = repository.login(
      new SimpleCredentials("admin", "admin".toCharArray))

    def userManager = {
      session.asInstanceOf[JackrabbitSession].getUserManager()
    }

    def valueFactory = {
      session.getValueFactory()
    }

  }

}