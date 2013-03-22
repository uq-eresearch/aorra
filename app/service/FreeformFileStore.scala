package service

import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import javax.jcr.Session
import org.apache.jackrabbit.api.JackrabbitSession
import play.api.{Application,Plugin}
import org.apache.jackrabbit.api.security.user.Group
import java.security.Principal
import org.apache.jackrabbit.api.security.user.UserManager
import javax.jcr.Node
import javax.jcr.nodetype.NodeType
import scala.collection.JavaConversions._
import javax.jcr.security.AccessControlList
import javax.jcr.security.Privilege.{JCR_READ, JCR_WRITE}
import org.apache.jackrabbit.api.security.user.Authorizable

class FreeformFileStore(implicit val application: Application) extends Plugin {

  override def onStart {
    initFileStore
  }

  protected def initFileStore {
    Jcr.session { session =>
      val group = GroupManager(session)

      val rs = group("Reef Secretariat")
      rs belongsTo group("Catchment Loads")
      rs belongsTo group("Groundcover")
      rs belongsTo group("Management Practices")
      rs belongsTo group("Marine")
    }
  }

  case class GroupManager(session: Session) {
    val fileStorePath = "filestore"
    val groupPath = "contributionGroups"

    val accessManager = session.getAccessControlManager
    val userManager = session.asInstanceOf[JackrabbitSession].getUserManager

    def apply(name: String): ContributionGroup = {
      // Defining an implicit conversion so we can just use JCR_READ, etc.
      implicit def str2privilege(s: String) = accessManager.privilegeFromName(s)
      // Get or create group
      val group = Option(userManager.getAuthorizable(name)) match {
        case Some(g: Group) => g
        case None => userManager.createGroup(NamedPrincipal(name), groupPath)
        case Some(a: Authorizable) =>
          sys.error(s"Authorizable already exists! Not a group: $a")
      }
      // Create the group folder
      val folder = rootFolder.findOrCreateFolder(name)
      // Set appropriate ACLs
      accessManager.getApplicablePolicies(folder.node.getPath).toSeq match {
        case Seq(acl: AccessControlList) =>
          acl.getAccessControlEntries() foreach {
            acl.removeAccessControlEntry(_)
          }
          acl.addAccessControlEntry(group.getPrincipal(),
              Array(JCR_READ, JCR_WRITE))
      }
      ContributionGroup(group)
    }

    lazy val rootFolder = {
      FolderHelper(session.getRootNode()).findOrCreateFolder(fileStorePath)
    }

    case class FolderHelper(node: Node) {
      def findOrCreateFolder(path: String) = {
        FolderHelper(if (node.hasNode(path)) {
          node.getNode(path)
        } else {
          node.addNode(path, NodeType.NT_FOLDER)
        })
      }
    }

    case class NamedPrincipal(name: String) extends Principal {
      def getName = name
    }

    case class ContributionGroup(group: Group) {
      def belongsTo(other: ContributionGroup) {
        if (!other.group.isMember(group)) {
          other.group.addMember(group)
        }
      }
    }
  }



}

