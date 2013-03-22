package test.service

import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.test.Helpers._
import play.api.test._
import service.Jcr
import test.AorraTestUtils.fakeApp
import org.apache.jackrabbit.api.JackrabbitSession
import org.apache.jackrabbit.api.security.user.Group
import service.FreeformFileStore
import org.specs2.matcher.DataTables

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
          Jcr.session { session =>
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

}