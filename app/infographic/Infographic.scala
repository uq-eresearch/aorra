package infographic

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import scala.collection.JavaConversions._
import java.util.UUID
import scala.util.Try
import org.jsoup.Jsoup
import play.api.libs.json._
import charts.Region
import charts.builder.spreadsheet.MarineBuilder
import charts.graphics.BeerCoaster
import charts.graphics.ProgressTable
import charts.builder.spreadsheet.ProgressTableBuilder
import play.api.templates.JavaScript

object Infographic {

  def apply(data: InfographicData): JavaScript = {
    views.js.InfographicController.data(
      JsString(data.baseYear),
      JsString(data.reportYears),
      MarineJsonBuilder(data.marineCharts),
      ManagementJsonBuilder(data.progressTables),
      CatchmentJsonBuilder(data.progressTables),
      data.marineCaptions.map(MarineCaptionsJsonBuilder(_))
        .getOrElse(Json.obj())
    )
  }

  def parseConfig(input: InputStream): Option[InfographicConfig] = {
    val yaml = new Yaml
    yaml.load(input) match {
      // SnakeYAML always uses [String, Object] for maps, so ignore warning
      case map: java.util.Map[String @unchecked, Any @unchecked] =>
        def containsYear(v: Any) = v.toString.matches(".*\\d+")
        def isUUID(v: Any) = Try(UUID.fromString(v.toString)).isSuccess
        Option(map.toMap)
          .filter( m => m.get("base_year").filter(containsYear).isDefined )
          .filter( m => m.get("report_year").filter(containsYear).isDefined)
          .filter( m => m.get("marine_spreadsheet_id").filter(isUUID).isDefined )
          .filter( m => m.get("progress_spreadsheet_id").filter(isUUID).isDefined )
          .filter( m => m.get("marine_captions_id").filter(isUUID).isDefined )
          .map { m =>
            InfographicConfig(
              m("base_year").toString,
              m("report_year").toString,
              m("marine_spreadsheet_id").toString,
              m("progress_spreadsheet_id").toString,
              m("marine_captions_id").toString
            )
          }
      case _ =>
        None
    }

  }

  private def toJson(regions: Seq[Option[(Region, JsValue)]]): JsValue =
    JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    })


  object MarineJsonBuilder {

    def apply(chartList: Seq[charts.Chart]): JsValue =
      toJson(chartList.map(hdl(_)))

    private def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
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

    private def category2str(category: BeerCoaster.Category): String = {
      import charts.graphics.BeerCoaster.Category._
      category match {
        case WATER_QUALITY  => "water"
        case CORAL          => "coral"
        case SEAGRASS       => "seagrass"
      }
    }

    private def indicator2str(indicator: BeerCoaster.Indicator): String = {
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
  }


  object ManagementJsonBuilder extends ProgressJson {

    def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
      import ProgressTableBuilder.ProgressTableChart
      val dataset = chart.asInstanceOf[ProgressTableChart].dataset

      implicit def ptdWrites = getPtdWrites(Set(
        "grazing", "sugarcane", "horticulture"
      ))

      Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
    }

  }

  object CatchmentJsonBuilder extends ProgressJson {

    def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
      import ProgressTableBuilder.ProgressTableChart
      val dataset = chart.asInstanceOf[ProgressTableChart].dataset

      implicit def ptdWrites = getPtdWrites(Set(
        "groundcover", "nitrogen", "sediment", "pesticides"
      ))

      Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
    }

  }

  object MarineCaptionsJsonBuilder {

    def apply(data: InputStream): JsValue = {
      val regions = Map(
        "gbr" -> "Great Barrier Reef",
        "cape-york" -> "Cape York",
        "wet-tropics" -> "Wet Tropics",
        "burdekin" -> "Burdekin",
        "mackay-whitsunday" -> "Mackay-Whitsunday",
        "fitzroy" -> "Fitzroy",
        "burnett-mary" -> "Burnett-Mary"
      )
      val doc = Jsoup.parse(data, "UTF-8", "/")
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


  trait ProgressJson {

    def apply(chartList: Seq[charts.Chart]): JsValue =
      toJson(chartList.map(hdl(_)))

    def hdl(chart: charts.Chart): Option[(Region, JsValue)]

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
  }

}

case class InfographicConfig(
  val baseYear: String,
  val reportYears: String, // May just be one year
  val marineChartFileId: String,
  val progressTableFileId: String,
  val marineCaptionsFileId: String
)

case class InfographicData(
  val baseYear: String,
  val reportYears: String, // May just be one year
  val marineCharts: Seq[charts.Chart],
  val progressTables: Seq[charts.Chart],
  val marineCaptions: Option[InputStream]
)