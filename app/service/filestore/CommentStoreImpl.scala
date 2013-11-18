package service.filestore

import java.util.{List => JList, SortedSet => JSortedSet}
import scala.beans.BeanProperty
import org.jcrom.Jcrom
import javax.jcr.Session
import javax.jcr.nodetype.NodeType
import java.util.Calendar
import models.{CommentDAO, Comment => CommentModel}
import scala.util.Try
import com.google.common.collect.ImmutableSortedSet
import org.apache.jackrabbit.commons.JcrUtils

class CommentStoreImpl(val jcrom: Jcrom, initialSession: Session)
  extends CommentStore {

  JcrUtils.getOrCreateByPath(CommentDAO.BASE_PATH,
    NodeType.NT_UNSTRUCTURED, initialSession);

  override def getManager(session: Session) =
    new CommentManagerImpl(jcrom, session)

}

class CommentManagerImpl(val jcrom: Jcrom, val session: Session)
    extends CommentStore.Manager {

  import service.filestore.CommentStore.Comment

  lazy val dao = new CommentDAO(session, jcrom)

  override def create(userId: String, targetId: String, msg: String) =
    toComment(create(new CommentModel(userId, targetId, msg)))

  override def findById(id: String): Comment =
    Try(dao.loadById(id)).map(toComment(_)).getOrElse(null)

  override def findByTarget(targetId: String): JSortedSet[Comment] = {
    import scala.collection.JavaConversions.{asScalaBuffer,asJavaCollection}
    val found: List[CommentModel] = dao.findAll(CommentDAO.BASE_PATH) match {
      case null => Nil
      case jl: JList[CommentModel] =>
        asScalaBuffer(jl).toList.filter(_.getTargetId == targetId)
    }
    ImmutableSortedSet.copyOf[Comment](
        asJavaCollection(found.map(toComment)))
  }

  override def update(comment: Comment): Comment = {
    val m = dao.loadById(comment.getId)
    m.setMessage(comment.getMessage)
    toComment(update(m))
  }

  override def delete(comment: Comment): Unit =
    dao.removeById(comment.getId)

  def toComment(m: CommentModel): Comment = {
    new CommentImpl(m.getId, m.getUserId, m.getTargetId,
        m.getCreated, m.getLastModified, m.getMessage)
  }

  // Create and load (to get created & modified)
  def create(m: CommentModel): CommentModel = dao.loadById(dao.create(m).getId)

  // Update and load (to get created & modified)
  def update(m: CommentModel): CommentModel = dao.loadById(dao.update(m).getId)

}

final object CommentImpl {
  import service.filestore.CommentStore.Comment
  val ordering = Ordering.by { (x: Comment) => (x.getCreationTime, x.getId) }
}

final class CommentImpl(
    @BeanProperty val id: String,
    @BeanProperty val userId: String,
    @BeanProperty val targetId: String,
    @BeanProperty val creationTime: Calendar,
    @BeanProperty val modificationTime: Calendar,
    @BeanProperty var message: String)
    extends CommentStore.Comment with Ordered[CommentStore.Comment] {

  override def equals(other: Any) = other match {
    case that: CommentStore.Comment => this.getId == that.getId
    case _ => false
  }

  override lazy val hashCode = getId.hashCode

  override def compare(that: CommentStore.Comment) =
    CommentImpl.ordering.compare(this, that)

  override def toString = s"($id) $userId: $message"

}