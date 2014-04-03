package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{future, Future}
import java.io.InputStream
import org.jcrom.Jcrom
import com.google.inject.Inject
import charts.builder.FileStoreDataSourceFactory.getDataSource
import controllers.ScalaSecured.isAuthenticatedAsync
import javax.jcr.Session
import models.CacheableUser
import play.api.mvc.Controller
import play.libs.F
import org.yaml.snakeyaml.Yaml
import service.filestore.FileStore
import org.apache.commons.io.IOUtils
import scala.collection.JavaConversions.{mapAsScalaMap, iterableAsScalaIterable}
import play.api.templates.JavaScript
import charts.ChartType
import charts.Region
import charts.builder.ChartBuilder
import charts.builder.spreadsheet.MarineBuilder
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import charts.graphics.BeerCoaster
import play.api.libs.json.JsString
import play.api.libs.json.JsObject
import play.api.libs.json.Writes
import play.api.libs.json.JsValue
import play.api.libs.json.JsNull

class InfographicController @Inject()(
      val jcrom: Jcrom,
      val filestore: service.filestore.FileStore,
      val sessionFactory: service.JcrSessionFactory,
      val chartBuilder: ChartBuilder)
    extends Controller {

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
            val yaml = new Yaml
            val document = IOUtils.toString(file.getData)
            yaml.load(document) match {
              case map: java.util.Map[String, Any] =>
                Ok(infographicJs(fsm, map.toMap)).as("application/javascript")
              case _ =>
                NotFound
            }
          case _ => NotFound
        }
      })
    }
  }

  private def infographicJs(
      fsm: FileStore.Manager,
      yamlDoc: Map[String, Any]): JavaScript = {
    def getId(key: String): Option[String] = {
      val id = yamlDoc(key).asInstanceOf[String]
      fsm.getByIdentifier(id) match {
        case file: FileStore.File => Some(file.getIdentifier)
        case _ => None
      }
    }

    views.js.InfographicController.data(
      yamlDoc("base_year").toString(),
      getId("marine_spreadsheet_id").flatMap(marineJson(_))
    )
  }

  def category2str(category: BeerCoaster.Category): String = {
    import charts.graphics.BeerCoaster.Category._
    category match {
      case WATER_QUALITY  => "water"
      case CORAL          => "coral"
      case SEAGRASS       => "seagrass"
    }
  }

  def indicator2str(indicator: BeerCoaster.Indicator): String = {
    import charts.graphics.BeerCoaster.Indicator._
    indicator match {
      case CHLOROPHYLL_A          => "water-chlorophyll"
      case TOTAL_SUSPENDED_SOLIDS => "water-solids"
      case SETTLEMENT             => "coral-change"
      case JUVENILE               => "coral-juvenile"
      case ALGAE                  => "coral-macroalgae"
      case COVER                  => "coral-cover"
      case ABUNDANCE              => "seagrass-abundance"
      case REPRODUCTION           => "seagrass-reproduction"
      case NUTRIENT_STATUS        => "seagrass-nutrient"
    }
  }


  def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
    import MarineBuilder.MarineChart
    val beercoaster = chart.asInstanceOf[MarineChart].beercoaster()

    implicit val bccWrites: Writes[BeerCoaster.Condition] = new Writes[BeerCoaster.Condition] {
      override def writes(condition: BeerCoaster.Condition) =
        Json.obj("qualitative" -> asJsValue(condition))

      def asJsValue(condition: BeerCoaster.Condition): JsValue = condition match {
        case BeerCoaster.Condition.NOT_EVALUATED => JsNull
        case _ => JsString(condition.getLabel())
      }
    }

    implicit val bcWrites: Writes[BeerCoaster] = new Writes[BeerCoaster] {
      override def writes(beercoaster: BeerCoaster) = {
        var jsObj = Json.obj("overall" -> beercoaster.getOverallCondition())
        jsObj = BeerCoaster.Category.values.foldLeft(jsObj) { (o, c) =>
          o ++ Json.obj(category2str(c) -> beercoaster.getCondition(c))
        }
        jsObj = BeerCoaster.Indicator.values.foldLeft(jsObj) { (o, c) =>
          o ++ Json.obj(indicator2str(c) ->beercoaster.getCondition(c))
        }
        jsObj
      }
    }

    Some((chart.getDescription().getRegion(), Json.toJson(beercoaster)))
  }

  private def marineJson(fileId: String): Option[JsValue] = {

    val charts = { chartBuilder.getCharts(fileId, ChartType.MARINE, null, null) }

    val regions: Seq[Option[(Region, JsValue)]] = charts.toList.map(hdl(_))

    Some(JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    }))
  }



}