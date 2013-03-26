package test.service

import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import java.util.Iterator
import javax.jcr.Node
import javax.jcr.Session
import javax.jcr.nodetype.NodeType
import org.apache.jackrabbit.api.JackrabbitSession
import org.apache.jackrabbit.api.security.user.Group
import org.specs2.matcher.DataTables
import org.specs2.mutable._
import org.specs2.specification.Scope
import play.api.Play
import play.api.test.Helpers._
import play.api.test._
import scala.collection.JavaConversions._
import service.FreeformFileStore
import test.AorraTestUtils.fakeApp
import service.JcrSessionFactory
import play.libs.F
import com.wingnest.play2.jackrabbit.Jcr
import javax.jcr.security.AccessControlEntry
import javax.jcr.security.AccessControlList
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils
import javax.jcr.nodetype.NodeType

/**
 * Check that Jackrabbit is hooked up properly for testing.
 */
class FreeformFileStoreSpec extends Specification with DataTables {

  "Freeform File Store" should {

    "create groups and folders for file storage" in {
      "group name"           |>
      "Reef Secretariat"     |
      "Catchment Loads"      |
      "Groundcover"          |
      "Management Practices" |
      "Marine"               | { groupName =>
        running(fakeApp) {
          try {
            sf.inSession { session: Session =>
              val um = session.asInstanceOf[JackrabbitSession].getUserManager()
              // Check group exists
              val g = um.getAuthorizable(groupName)
              g must not be null
              g must beAnInstanceOf[Group]
              g.getID should_== groupName
              // Check folder exists
              val root = session.getRootNode()
                .getNode(FreeformFileStore.FILE_STORE_PATH)
              root.hasNode(groupName) must beTrue
              val folder = root.getNode(groupName)
              folder.getPrimaryNodeType.getName must_== "nt:folder"
              // Check ACL permits the group, but not everyone
              val entry = findAclEntry(session, folder , groupName)
              entry must beSome[AccessControlEntry]
              val privilegeNames = entry.get
                .getPrivileges.map(_.getName).toSet
              privilegeNames.must_==(Set("jcr:read", "jcr:addChildNodes",
                  "jcr:removeChildNodes", "jcr:modifyProperties"))
              findAclEntry(session, folder, "everyone") must beNone
            }
          } catch {
            case e: RuntimeException => throw e.getCause()
          }
          true
        }
      }
    }

    "provide a set of contribution folders" in {
      running(fakeApp) {
        sf.inSession { session: Session =>
          val folders = Play.current.plugin(classOf[FreeformFileStore])
            .get.getAll(session);
          val folderNames = folders map { _.getName() }
          folderNames must contain("Reef Secretariat")
          folderNames must contain("Catchment Loads")
          folderNames must contain("Groundcover")
          folderNames must contain("Management Practices")
          folderNames must contain("Marine")
        }
      }
    }

    "sets permissions for contribution folders" in {
      "group name"           |>
      "Catchment Loads"      |
      "Groundcover"          |
      "Management Practices" |
      "Marine"               | { groupName =>
        running(fakeApp) {
          try {
            val plugin = Play.current.plugin(classOf[FreeformFileStore]).get
            // First, check admin has permissions for everything
            sf.inSession { session: Session =>
              val um = session.asInstanceOf[JackrabbitSession].getUserManager
              val allFolderNames = plugin.getAll(session) map { _.getName() }
              val folderNames = plugin.getWritable(session) map { _.getName() }
              folderNames must_== allFolderNames
              folderNames must contain(groupName)
            }
            // Check that group members have appropriate privileges
            val creds = sf.inSession { session: Session =>
              val um = session.asInstanceOf[JackrabbitSession].getUserManager
              val user = um.createUser("foo", "bar")
              val group = um.getAuthorizable(groupName).asInstanceOf[Group]
              group.addMember(user)
              user.getCredentials
            }
            sf.inSession(creds, { session: Session =>
              val um = session.asInstanceOf[JackrabbitSession].getUserManager
              val user = um.getAuthorizable(session.getUserID)
              user.memberOf.map(_.getPrincipal.getName) must contain(groupName)
              val allFolderNames = plugin.getAll(session) map { _.getName() }
              allFolderNames must have(_.size > 1)
              allFolderNames must contain(groupName)
              val folderNames = plugin.getWritable(session) map { _.getName() }
              folderNames must contain(groupName)
              folderNames must have size(1)
            })
          } catch {
            case e: RuntimeException => throw e.getCause()
          }
        }
      }
    }

  }

  // Session handling
  val sf: JcrSessionFactory = new JcrSessionFactory() {
    def newAdminSession() = Jcr.login("admin", "admin")
  }
  implicit def scala2ffunc[A, R](f: (A => R)): F.Function[A,R] = {
    new F.Function[A,R] { def apply(a: A): R = f(a) }
  }

  /*
   * Helper method for finding a particular ACL entry on a node.
   */
  def findAclEntry(session: Session, node: Node, principalName: String) = {
    val acm = session.getAccessControlManager
    val acl = AccessControlUtils.getAccessControlList(acm, node.getPath)
    val entries = acl.getAccessControlEntries.toIterator
    entries.find(_.getPrincipal.getName == principalName)
  }

}