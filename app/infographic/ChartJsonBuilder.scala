package infographic

import play.api.libs.json._
import charts.Region

trait ChartJsonBuilder {

  def toJson(regions: Seq[Option[(Region, JsValue)]]): JsValue =
    JsObject(regions.flatten.map { p =>
      val regionName = p._1.toString().toLowerCase().replaceAll("_", "-")
      (regionName, p._2)
    })

}