package charts.builder.spreadsheet.external

import charts.builder.FileStoreDataSourceFactory.getDataSource
import charts.builder.spreadsheet.SpreadsheetDataSource
import service.filestore.FileStore
import service.JcrSessionFactory
import com.google.inject.Inject
import play.api.{Logger => logger}
import play.libs.F
import javax.jcr.Session
import org.jcrom.util.JcrUtils
import org.jcrom.util.PathUtils
import scala.collection.mutable.{Map => MutableMap}

class FileStoreExternalCellRefResolver @Inject()(
    sessionFactory: JcrSessionFactory,
    filestore: FileStore,
    jackrabbitUser: String) extends ExternalCellRefResolver {

  override def resolve(
      base: DestinationIdentifier,
      refs: Set[UnresolvedRef]
      ): Set[ResolvedRef] =
    withFileStoreManager(jackrabbitUser) { (fsm) =>
      val resolver = getCachingResolver(fsm, base)
      // Resolve references
      refs.map { (ref) =>
        ResolvedRef(resolver(ref.source).map(getDataSource(_)), ref.link)
      }
    }

  def getCachingResolver(
      fsm: FileStore.Manager,
      base: DestinationIdentifier): String => Option[FileStore.File] = {
    val fsmh = new FsmHelper(fsm)
    // Resolve base file
    val baseFile = fsmh.getFileById(base).getOrElse {
      throw new Exception("Base file not found.")
    }
    val results = MutableMap[String, Option[FileStore.File]]()
    (source: String) => {
      results.getOrElse(source, {
        val file = fsmh.getFileById(source).orElse {
          // Otherwise try resolving the path
          fsmh.getFileByPath(baseFile, source)
        }
        if (file.isEmpty) {
          logger.debug(s"Unable to resolve $source")
        }
        results += (source -> file)
        file
      })
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
