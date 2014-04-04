package infographic

import play.api.libs.json.JsValue
import charts.builder.spreadsheet.ProgressTableBuilder
import charts.Region
import play.api.libs.json.Json


object ManagementJsonBuilder extends ProgressJsonBuilder {

  def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
    import ProgressTableBuilder.ProgressTableChart
    val dataset = chart.asInstanceOf[ProgressTableChart].dataset

    implicit def ptdWrites = getPtdWrites(Set(
      "grazing", "sugarcane", "horticulture"
    ))

    Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
  }

}