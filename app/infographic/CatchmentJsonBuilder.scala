package infographic

import play.api.libs.json._
import charts.builder.spreadsheet.ProgressTableBuilder
import charts.Region


object CatchmentJsonBuilder extends ProgressJsonBuilder {

  def hdl(chart: charts.Chart): Option[(Region, JsValue)] = {
    import ProgressTableBuilder.ProgressTableChart
    val dataset = chart.asInstanceOf[ProgressTableChart].dataset

    implicit def ptdWrites = getPtdWrites(Set(
      SimpleIndicator("groundcover"),
      SimpleIndicator("nitrogen"),
      SimpleIndicator("sediment"),
      SimpleIndicator("pesticides")
    ))

    Some((chart.getDescription().getRegion(), Json.toJson(dataset)))
  }

}