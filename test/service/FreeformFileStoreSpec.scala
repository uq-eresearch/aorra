package test.service

import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.test.Helpers._
import play.api.test._
import service.Jcr
import test.AorraTestUtils.fakeApp
import org.apache.jackrabbit.api.JackrabbitSession
import org.apache.jackrabbit.api.security.user.Group

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class FreeformFileStoreSpec extends Specification {

  "Freeform File Store" should {

    "create groups for file storage" in {
      running(fakeApp) {
        Jcr.session { session =>
          val um = session.asInstanceOf[JackrabbitSession].getUserManager()
          val name = "Reef Secretariat"
          val g = um.getAuthorizable(name)
          g must not be null
          g must beAnInstanceOf[Group]
          g.getID should_== name
        }
      }
    }
  }

}