package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{future, Future}
import java.io.InputStream
import org.jcrom.Jcrom
import com.google.inject.Inject
import charts.builder.FileStoreDataSourceFactory.getDataSource
import charts.builder.spreadsheet.external._
import controllers.ScalaSecured.isAuthenticatedAsync
import javax.jcr.Session
import models.CacheableUser
import play.api.mvc.Controller
import play.libs.F
import service.filestore.FileStore

class SpreadsheetController @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory)
    extends Controller {

  import scala.language.implicitConversions

  // For JcrSessionFactory interoperability
  implicit private def toPlayFunc[A, B](f: A => B): F.Function[A, B] =
    new F.Function[A, B] {
      def apply(a: A): B = f(a)
    }


  def hasExternalRefs(fileId: String) = isAuthenticatedAsync { user => implicit request =>
    val yes = Ok
    val no = NotFound
    future {
      retrieveDatasource(fileId, user) match {
        case Some(ds) => if (detector.hasAny(ds)) yes else no
        case None => no
      }
    }
  }

  def updateExternalRefs(fileId: String) = isAuthenticatedAsync { user => implicit request =>
    retrieveDatasource(fileId, user) match {
      case Some(ds) =>
        future {
          val unresolvedRefs = detector.scan(ds)
          val resolvedRefs = resolver(user).resolve(fileId, unresolvedRefs)
          replacer.replace(ds, resolvedRefs) match {
            case None => Ok // File is unchanged - no write required.
            case Some(replacement: InputStream) =>
              val updatedFile = writeContent(fileId, user, replacement)
              val loc = controllers.routes.FileStoreController.downloadFile(
                    updatedFile.getIdentifier(),
                    updatedFile.getVersions().last().getName()).url
              Created.withHeaders("Location" -> loc)
          }
        }
      case None => Future.successful(NotFound)
    }
  }


  private def detector: ExternalCellRefDetector =
    SpreadsheetDataSourceExternalCellRefDetector

  private def resolver(user: CacheableUser): ExternalCellRefResolver =
    new FileStoreExternalCellRefResolver(
        sessionFactory, filestore, user.getJackrabbitUserId)

  private def replacer: ExternalCellRefReplacer = ???

  private def retrieveDatasource(fileId: String, user: CacheableUser) =
    sessionFactory.inSession(user.getJackrabbitUserId, { (session: Session) =>
      val fsm = filestore.getManager(session)
      fsm.getByIdentifier(fileId) match {
        case file: FileStore.File => Option(getDataSource(file))
        case _ => None
      }
    })

  private def writeContent(
      fileId: String, user: CacheableUser,
      newContent: InputStream): FileStore.File = {
    sessionFactory.inSession(user.getJackrabbitUserId, { (session: Session) =>
      val fsm = filestore.getManager(session)
      fsm.getByIdentifier(fileId) match {
        case file: FileStore.File =>
          file.update(file.getMimeType, newContent)
        case fof =>
          throw new RuntimeException("The source file has disappeared: "+fof)
      }
    })
  }

}