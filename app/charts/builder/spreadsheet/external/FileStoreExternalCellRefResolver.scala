package charts.builder.spreadsheet.external

import scala.concurrent.ExecutionContext.Implicits.global
import charts.builder.FileStoreDataSourceFactory.getDataSource
import charts.builder.spreadsheet.SpreadsheetDataSource
import scala.concurrent.future
import scala.concurrent.Future
import service.filestore.FileStore
import service.JcrSessionFactory
import com.google.inject.Inject
import play.libs.F
import javax.jcr.Session
import org.jcrom.util.JcrUtils
import org.jcrom.util.PathUtils

class FileStoreExternalCellRefResolver @Inject()(
    sessionFactory: JcrSessionFactory,
    filestore: FileStore,
    jackrabbitUser: String) extends ExternalCellRefResolver {

  override def resolve(
      base: DestinationIdentifier,
      refs: Set[UnresolvedRef]
      ): Future[Set[ResolvedRef]] = future {
    withFileStoreManager(jackrabbitUser) { (fsm) =>
      val fsmh = new FsmHelper(fsm)
      // Resolve base file
      val baseFile = fsmh.getFileById(base).getOrElse {
        throw new Exception("Base file not found.")
      }
      // Resolve references
      refs.map { (ref) =>
        val resolvedFile: Option[FileStore.File] =
          // Try ID first
          fsmh.getFileById(ref.source).orElse {
            // Otherwise try resolving the path
            fsmh.getFileByPath(baseFile, ref.source)
          }
        ResolvedRef(resolvedFile.map(getDataSource(_)), ref.link)
      }
    }
  }


  private class FsmHelper(fsm: FileStore.Manager) {

    def getFileById(id: String): Option[FileStore.File] =
      fsm.getByIdentifier(id) match {
        case file: FileStore.File => Some(file)
        case _ => None
      }

    def getFileByPath(
        file: FileStore.File, path: String): Option[FileStore.File] = {
      def followPath(
          folder: FileStore.Folder,
          parts: Seq[String]): Option[FileStore.File] = {
        folder.getFileOrFolder(parts.head) match {
          case null => None
          case file: FileStore.File =>
            if (parts.tail.isEmpty)
              Some(file)
            else
              None
          case subFolder: FileStore.Folder =>
            if (parts.tail.isEmpty)
              None
            else
              followPath(subFolder, parts.tail)
        }
      }
      val baseFolder = file.getParent
      val parts = path.split("/")
      followPath(baseFolder, parts)
    }

  }

  private def withFileStoreManager[A](
      userId: String)(op: FileStore.Manager => A): A = {
    sessionFactory.inSession(jackrabbitUser, toPlayFunc({ (session: Session) =>
      op(filestore.getManager(session))
    }))
  }

  // For JcrSessionFactory interoperability
  private def toPlayFunc[A, B](f: A => B): F.Function[A, B] =
    new F.Function[A, B] {
      def apply(a: A): B = f(a)
    }

}
