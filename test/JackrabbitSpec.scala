package test

import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import javax.jcr.Repository
import org.apache.jackrabbit.core.TransientRepository
import javax.jcr.Credentials
import javax.jcr.GuestCredentials
import javax.jcr.RepositoryException
import org.apache.commons.io.FileUtils
import org.specs2.specification.Scope
import java.io.File

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class JackrabbitSpec extends Specification {

  val REPOSITORY_CONFIG_PATH = "test/repository.xml"
  val REPOSITORY_DIRECTORY_PATH = "./target/jackrabbittestrepository"

  "Jackrabbit" should {

    "be able to start" in new CleanEnvironment {
      try {
        val repository = new TransientRepository(
              REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH)
        val session = repository.login(new GuestCredentials(), "default")
        // The default identifier is apparently a "cafe babe"
        session.getRootNode().getIdentifier() must startWith("cafebabe")
        session.logout()
      } catch {
        case e: RepositoryException =>
          e.printStackTrace(System.out)
          failure(e.getMessage())
      }
    }
  }

  trait CleanEnvironment extends Scope with BeforeAfter {
    import FileUtils.deleteDirectory
    def before = if (repositoryDir.exists()) { deleteDirectory(repositoryDir) }
    def after = {
      repositoryDir.exists.should_==(true)
      deleteDirectory(repositoryDir)
      repositoryDir.exists.should_==(false)
    }
    private def repositoryDir = new File(REPOSITORY_DIRECTORY_PATH)
  }
}