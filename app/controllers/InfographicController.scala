package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{future, Future}
import com.google.inject.Inject
import org.jcrom.Jcrom
import charts.builder.ChartBuilder
import play.api.mvc.Controller
import play.libs.F
import service.filestore.FileStore
import infographic._
import charts.ChartType
import scala.collection.JavaConversions._

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

  def dataFile(fileId: String) = isAuthenticatedAsync { user => implicit request =>
    future {
      sessionFactory.inSession(user.getJackrabbitUserId, { session: Session =>
        val fsm = filestore.getManager(session)
        fsm.getByIdentifier(fileId) match {
          case file: FileStore.File if file.getMimeType == YAML_MIMETYPE =>
            Infographic.parseConfig(file.getData) match {
              case Some(config) =>
                Ok(Infographic(config2data(fsm, config)))
                  .as("application/javascript")
              case _ =>
                NotFound
            }
          case _ => NotFound
        }
      })
    }
  }

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