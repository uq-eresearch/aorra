package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import charts.builder.FileStoreDataSourceFactory.getDataSource
import org.jcrom.Jcrom
import com.google.inject.Inject
import controllers.ScalaSecured.isAuthenticatedAsync
import charts.builder.spreadsheet.SpreadsheetDataSource
import charts.builder.spreadsheet.external.ExternalCellRefDetector
import play.api.mvc.Controller
import scala.concurrent.Future
import play.api.mvc.SimpleResult
import models.CacheableUser
import play.libs.F
import javax.jcr.Session
import service.filestore.FileStore
import charts.builder.spreadsheet.external.ExternalCellRefResolver
import charts.builder.spreadsheet.external.ExternalCellRefReplacer
import java.io.InputStream

class SpreadsheetController @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory)
    extends Controller {

  import scala.language.implicitConversions

  // For JcrSessionFactory interoperability
  implicit private def toPlayFunc[A, B](f: A => B): F.Function[A, B] =
    new F.Function[A, B] { def apply(a: A): B = f(a) }

  def hasExternalRefs(fileId: String) = isAuthenticatedAsync { user => implicit request =>
    val yes = Ok
    val no = NotFound
    retrieveDatasource(fileId, user) match {
      case Some(ds) => detector.hasAny(ds).map(if (_) yes else no)
      case None => Future.successful(no)
    }
  }

  def updateExternalRefs(fileId: String) = isAuthenticatedAsync { user => implicit request =>
    retrieveDatasource(fileId, user) match {
      case Some(ds) =>
        detector.scan(ds)
          .flatMap(resolver(user).resolve(fileId, _))
          .flatMap(replacer.replace(ds, _))
          .map(_ match {
            case None => Ok // File is unchanged - no write required.
            case Some(replacement: InputStream) =>
              val updatedFile = writeContent(fileId, user, replacement)
              val loc = controllers.routes.FileStoreController.downloadFile(
                    updatedFile.getIdentifier(),
                    updatedFile.getVersions().last().getName()).url
              Created.withHeaders("Location" -> loc)
          })
      case None => Future.successful(NotFound)
    }
  }


  private def detector: ExternalCellRefDetector = ???

  private def resolver(user: CacheableUser): ExternalCellRefResolver = ???

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