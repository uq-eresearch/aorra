package infographic

import java.io.InputStream
import java.util.UUID

import scala.collection.JavaConversions._
import scala.util.Try

import org.yaml.snakeyaml.Yaml

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