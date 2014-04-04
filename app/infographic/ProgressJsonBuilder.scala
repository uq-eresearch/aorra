package infographic

import play.api.libs.json.JsValue
import play.api.libs.json.Writes
import charts.graphics.ProgressTable
import charts.Region
import play.api.libs.json._

trait ProgressJsonBuilder extends ChartJsonBuilder {

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