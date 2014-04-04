package controllers

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.language.implicitConversions
import org.jcrom.Jcrom
import com.google.inject.Inject
import charts.ChartType
import charts.builder.ChartBuilder
import controllers.ScalaSecured.{isAuthenticated, isAuthenticatedAsync}
import infographic.Infographic
import infographic.InfographicConfig
import infographic.InfographicData
import javax.jcr.Session
import play.api.mvc.Controller
import play.libs.F
import service.filestore.FileStore
import play.api.mvc.SimpleResult
import models.CacheableUser
import play.api.libs.iteratee.Enumerator
import helpers.ZipHelper
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import play.api.mvc.EssentialAction
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import play.api.templates.JavaScript
import play.api.mvc.Result
import scala.concurrent.Future
import org.apache.commons.io.IOUtils

class InfographicController @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory,
      val chartBuilder: ChartBuilder)
    extends Controller {

  type FileId = String

  val YAML_MIMETYPE = "application/x-yaml"

  import scala.language.implicitConversions

  // For JcrSessionFactory interoperability
  implicit private def toPlayFunc[A, B](f: A => B): F.Function[A, B] =
    new F.Function[A, B] {
      def apply(a: A): B = f(a)
    }

  def staticFile(fileId: String, assetPath: String) =
    // Located in resources/infographic
    controllers.Assets.at(path="/infographic", assetPath)

  def dataFile(fileId: FileId) = isAuthenticatedAsync { user => implicit request =>
    future {
      withInfographicData[SimpleResult](user, fileId) { data =>
        Ok(Infographic(data)).as("application/javascript")
      }.getOrElse(NotFound)
    }
  }

  def archiveFile(fileId: FileId): EssentialAction = isAuthenticated { user => implicit request =>
    withInfographicData[SimpleResult](user, fileId) { data =>
      val archiveName = "infographic_"+data.reportYears
      def staticFile(filename: String) = {
        getClass.getResourceAsStream(s"/infographic/$filename")
      }
      val enumerator = zipEnumerator { zos =>
        // Dynamic data file
        zos.putArchiveEntry(new ZipArchiveEntry(s"$archiveName/data.js"))
        zos.write(Infographic(data).toString.getBytes)
        zos.closeArchiveEntry()
        // Static files
        val manifest: Seq[String] =
          IOUtils.toString(staticFile("manifest.txt"))
                 .split("\n")
        manifest.foreach { filename =>
          zos.putArchiveEntry(new ZipArchiveEntry(s"$archiveName/$filename"))
          zos.write(IOUtils.toByteArray(staticFile(filename)))
          zos.closeArchiveEntry()
        }
      }
      Ok.chunked(enumerator).withHeaders(
        "Content-Type" -> "application/zip",
        "Content-Disposition" -> ContentDispositionSupport.attachment(archiveName+".zip"),
        "Cache-Control" -> "max-age=0, must-revalidate")
    }.getOrElse(NotFound)
  }

  protected def zipEnumerator(f: (ZipArchiveOutputStream) => Unit) =
    Enumerator.outputStream { os =>
      val zip = ZipHelper.setupZipOutputStream(os)
      f(zip)
      zip.close()
    }

  def withInfographicData[R](user: CacheableUser, fileId: FileId)(op: InfographicData => R): Option[R] =
    sessionFactory.inSession(user.getJackrabbitUserId, { session: Session =>
      val fsm = filestore.getManager(session)
      fsm.getByIdentifier(fileId) match {
        case file: FileStore.File if file.getMimeType == YAML_MIMETYPE =>
          Infographic.parseConfig(file.getData) match {
            case Some(config) =>
              Some(op(config2data(fsm, config)))
            case _ =>
              None
          }
        case _ => None
      }
    })

  private def config2data(
      fsm: FileStore.Manager,
      config: InfographicConfig): InfographicData = {
    def checkIdExists(id: FileId): Option[FileId] = {
      fsm.getByIdentifier(id) match {
        case file: FileStore.File => Some(file.getIdentifier)
        case _ => None
      }
    }

    def getCharts(chartType: ChartType)(fileId: String): Seq[charts.Chart] =
        chartBuilder.getCharts(fileId, chartType, null, null).toList

    val marineCharts: Seq[charts.Chart] =
      checkIdExists(config.marineChartFileId)
        .map(getCharts(ChartType.MARINE))
        .getOrElse(Seq())

    val progressCharts: Seq[charts.Chart] =
      checkIdExists(config.progressTableFileId)
        .map(getCharts(ChartType.PROGRESS_TABLE_REGION))
        .getOrElse(Seq())

    InfographicData(
      config.baseYear,
      config.reportYears,
      marineCharts,
      progressCharts,
      checkIdExists(config.marineCaptionsFileId)
        .map(fsm.getByIdentifier(_).asInstanceOf[FileStore.File].getData)
    )
  }


}