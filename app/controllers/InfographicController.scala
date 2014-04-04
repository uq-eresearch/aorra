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
import charts.builder.spreadsheet.ProgressTableBuilder
import charts.graphics.ProgressTable
import org.jsoup.Jsoup
import infographic.Infographic
import infographic.InfographicConfig

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
            Infographic.parseConfig(file.getData) match {
              case Some(config) =>
                Ok(infographicJs(fsm, config)).as("application/javascript")
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
      config: InfographicConfig): JavaScript = {
    def getId(id: String): Option[String] = {
      fsm.getByIdentifier(id) match {
        case file: FileStore.File => Some(file.getIdentifier)
        case _ => None
      }
    }

    views.js.InfographicController.data(
      JsString(config.baseYear),
      JsString(config.reportYears),
      getId(config.marineChartFileId).map(marineJson(_))
        .getOrElse(Json.obj()),
      getId(config.progressTableFileId).map(managementJson(_))
        .getOrElse(Json.obj()),
      getId(config.progressTableFileId).map(catchmentJson(_))
        .getOrElse(Json.obj()),
      getId(config.marineCaptionsFileId).map(captionJson(fsm)(_))
        .getOrElse(Json.obj())
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


  def marineHdl(chart: charts.Chart): Option[(Region, JsValue)] = {
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

  def getPtdWrites(includeIndicators: Set[String]): Writes[ProgressTable.Dataset] = {
    import scala.collection.JavaConversions._
    new Writes[ProgressTable.Dataset] {
      override def writes(dataset: ProgressTable.Dataset) = {
        val row = dataset.rows.toSeq.head

        val indicators =
          dataset.columns.toList.zipWithIndex.flatMap { case (column, i) =>
            val indicator = column.header.toLowerCase
            val cell = row.cells.get(i)
            if (cell != null && includeIndicators.contains(indicator)) {
              val target = column.target
                .replaceAll("Target: ", "")
                .replaceAll(" per cent", "%")
                .replaceAll("\n", " ")
              Seq(indicator -> Json.obj(
                "qualitative" -> toJs(cell.condition),
                "quantitative" -> JsString(cell.progress),
                "target" -> JsString(target)
              ))
            } else {
              Seq()
            }
          }

        indicators.foldLeft(Json.obj()) { (o, v) =>
          o ++ Json.obj( v._1 -> v._2 )
        }
      }

      def toJs(condition: ProgressTable.Condition) = {
        condition match {
          case null => JsNull
          case ProgressTable.Condition.UNDECIDED => JsNull
          case _ => JsString(condition.toString())
        }
      }
    }
  }

  def managementHdl(chart: charts.Chart): Option[(Region, JsValue)] = {
    import ProgressTableBuilder.ProgressTableChart
    val dataset = chart.asInstanceOf[ProgressTableChart].dataset

    implicit def ptdWrites = getPtdWrites(Set(
      "grazing", "sugarcane", "horticulture"
    ))

    Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
  }

  def catchmentHdl(chart: charts.Chart): Option[(Region, JsValue)] = {
    import ProgressTableBuilder.ProgressTableChart
    val dataset = chart.asInstanceOf[ProgressTableChart].dataset

    implicit def ptdWrites = getPtdWrites(Set(
      "groundcover", "nitrogen", "sediment", "pesticides"
    ))

    Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
  }

  private def marineJson(fileId: String): JsValue = {

    val charts = chartBuilder.getCharts(fileId, ChartType.MARINE, null, null)

    val regions: Seq[Option[(Region, JsValue)]] = charts.toList.map(marineHdl(_))

    JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    })
  }

  private def managementJson(fileId: String): JsValue = {

    val charts = chartBuilder.getCharts(fileId, ChartType.PROGRESS_TABLE_REGION, null, null)

    val regions: Seq[Option[(Region, JsValue)]] = charts.toList.map(managementHdl(_))

    JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    })
  }

  private def catchmentJson(fileId: String): JsValue = {

    val charts = chartBuilder.getCharts(fileId, ChartType.PROGRESS_TABLE_REGION, null, null)

    val regions: Seq[Option[(Region, JsValue)]] = charts.toList.map(catchmentHdl(_))

    JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    })
  }

  private def captionJson(fsm: FileStore.Manager)(fileId: String): JsValue = {

    val file = fsm.getByIdentifier(fileId).asInstanceOf[FileStore.File]

    val regions = Map(
      "gbr" -> "Great Barrier Reef",
      "cape-york" -> "Cape York",
      "wet-tropics" -> "Wet Tropics",
      "burdekin" -> "Burdekin",
      "mackay-whitsunday" -> "Mackay-Whitsunday",
      "fitzroy" -> "Fitzroy",
      "burnett-mary" -> "Burnett-Mary"
    )

    val doc = Jsoup.parse(file.getData, "UTF-8", "/")

    JsObject(
      regions.toSeq.map { case (regionId, regionName) =>
        val searchStr = regionName.replaceAll("[\\- ]", ".")
        // Find header
        Option(doc.select("h3:matches("+searchStr+")").first)
          // Look for the next element
          .flatMap(e => Option(e.nextElementSibling))
          // It should be a <figure />
          .filter(e => e.tagName == "figure")
          // Find the caption
          .flatMap(e => Option(e.select("figcaption").first))
          // Get the text
          .map(_.text)
          // Output a pair with the text
          .map(text => (regionId, JsString(text)))
          // If it we didn't find what we were after, output nothing
          .getOrElse((regionId, JsString("")))
      }
    )
  }




}