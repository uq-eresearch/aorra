package infographic

import java.io.InputStream
import java.util.UUID

import scala.collection.JavaConversions._
import scala.util.Try

import org.yaml.snakeyaml.Yaml

import play.api.libs.json._
import play.api.templates.JavaScript

object Infographic {

  def apply(data: InfographicData): JsValue = {
    def stringMapToJsValue(m: Map[String,String]) =
      m.map { case (k, v) => (k, JsString(v)) }
    val regionNames = Map(
      "cape-york" -> "Cape York",
      "wet-tropics" -> "Wet Tropics",
      "burdekin" -> "Burdekin",
      "mackay-whitsunday" -> "Mackay-Whitsunday",
      "fitzroy" -> "Fitzroy",
      "burnett-mary" -> "Burnett-Mary"
    )
    val indicatorNames = Map(
      // Management
      "grazing" -> "Grazing",
      "sugarcane" -> "Sugarcane / Grains",
      "horticulture" -> "Horticulture",
      // Catchment
      "groundcover" -> "Groundcover",
      "nitrogen" -> "Nitrogen",
      "sediment" -> "Sediment",
      "pesticides" -> "Pesticides",
      // Marine
      "overall" -> "Overall Marine Condition",
      "coral" -> "Coral",
      "coral-change" -> "Coral change",
      "coral-cover" -> "Coral cover",
      "coral-juvenile" -> "Coral juvenile density",
      "coral-macroalgae" -> "Coral macroalgal cover",
      "seagrass" -> "Seagrass",
      "seagrass-abundance" -> "Seagrass abundance",
      "seagrass-reproduction" -> "Seagrass reproduction",
      "seagrass-nutrient" -> "Seagrass nutrients",
      "water" -> "Water Quality",
      "water-solids" -> "Total suspended solids",
      "water-chlorophyll" -> "Chlorophyll &alpha;"
    )

    Json.obj(
      "baseYear" -> JsString(data.baseYear),
      "reportYears" -> JsString(data.reportYears),
      "fullReportCardURL" -> JsString(data.fullReportCardURL),
      "otherReportCardsJSONP" -> JsString(data.otherReportCardsJSONP),
      "names" -> Map(
        "regions" -> regionNames,
        "indicators" -> indicatorNames
      ),
      "captions" -> Map(
        "marine" -> data.marineCaptions.map(MarineCaptionsJsonBuilder(_))
                        .getOrElse(Json.obj())
      ),
      "data" -> Map(
        "marine" -> MarineJsonBuilder(data.marineCharts),
        "management" -> ManagementJsonBuilder(data.progressTables),
        "catchment" -> CatchmentJsonBuilder(data.progressTables)
      )
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
          .filter( m => m.get("full_report_card_url").isDefined)
          .filter( m => m.get("other_report_cards_config_url").isDefined)
          .filter( m => m.get("marine_spreadsheet_id").filter(isUUID).isDefined )
          .filter( m => m.get("progress_spreadsheet_id").filter(isUUID).isDefined )
          .filter( m => m.get("marine_captions_id").filter(isUUID).isDefined )
          .map { m =>
            InfographicConfig(
              m("base_year").toString,
              m("report_year").toString,
              m("full_report_card_url").toString,
              m("other_report_cards_config_url").toString,
              m("marine_spreadsheet_id").toString,
              m("progress_spreadsheet_id").toString,
              m("marine_captions_id").toString
            )
          }
      case _ =>
        None
    }

  }

}

case class InfographicConfig(
  val baseYear: String,
  val reportYears: String, // May just be one year
  val fullReportCardURL: String,
  val otherReportCardsJSONP: String,
  val marineChartFileId: String,
  val progressTableFileId: String,
  val marineCaptionsFileId: String
)

case class InfographicData(
  val baseYear: String,
  val reportYears: String, // May just be one year
  val fullReportCardURL: String,
  val otherReportCardsJSONP: String,
  val marineCharts: Seq[charts.Chart],
  val progressTables: Seq[charts.Chart],
  val marineCaptions: Option[InputStream]
)