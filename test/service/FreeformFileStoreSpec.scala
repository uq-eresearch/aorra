package test.service

import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import javax.jcr.Session
import org.apache.jackrabbit.api.JackrabbitSession
import org.apache.jackrabbit.api.security.user.Group
import org.specs2.matcher.DataTables
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.Play
import play.api.test.Helpers._
import play.api.test._
import service.FreeformFileStore
import test.AorraTestUtils.fakeApp

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class FreeformFileStoreSpec extends Specification with DataTables {

  "Freeform File Store" should {

    "create groups for file storage" in {
      "group name"           |>
      "Reef Secretariat"     |
      "Catchment Loads"      |
      "Groundcover"          |
      "Management Practices" |
      "Marine"               | { groupName =>
        running(fakeApp) {
          inSession { session =>
            val um = session.asInstanceOf[JackrabbitSession].getUserManager()
            val g = um.getAuthorizable(groupName)
            g must not be null
            g must beAnInstanceOf[Group]
            g.getID should_== groupName
          }
        }
      }
    }
  }

  def inSession[A](op: (Session) => A): A = {
    val session = {
      def confStr(k: String) = Play.current.configuration.getString(k)
      com.wingnest.play2.jackrabbit.Jcr.login(
        confStr(ConfigConsts.CONF_JCR_USERID).get,
        confStr(ConfigConsts.CONF_JCR_PASSWORD).get)
    }
    try {
      op(session)
    } finally {
      session.logout
    }
  }

}