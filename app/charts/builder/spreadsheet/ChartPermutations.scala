package charts.builder.spreadsheet

import charts.ChartType
import charts.Region

import scala.collection.JavaConversions._
import java.lang.{Iterable => JIterable}
import java.util.{Iterator => JIterator, List => JList, Map => JMap}

case class ChartPermutation(
    chartType: ChartType, region: Region, parameters: Map[String, String]) {
  def javaParams: JMap[String, String] = parameters
}

class ChartPermutations(
    chartTypes: Seq[ChartType],
    regions: Seq[Region],
    supportedParameters: Map[String, Seq[String]])
    extends JIterable[ChartPermutation] {

  lazy val toSeq: Seq[ChartPermutation] =
    if (supportedParameters.forall(_._2.length == 1)) {
      // Extract singleton parameter values from lists
      val parameters = supportedParameters map {
        case (k, values) => k -> values.head
      }
      // Yield all the possible permutations of chart types and regions
      for {
        t <- chartTypes
        r <- regions
      } yield ChartPermutation(t, r, parameters)
    } else {
      // Get the parameter with the most possible values
      val (k, values) = supportedParameters.toList.sortBy(_._2.length).last
      values flatMap { v =>
        new ChartPermutations(
            chartTypes, regions,
            supportedParameters.updated(k, Seq(v))).toSeq
      }
    }

  def iterator: JIterator[ChartPermutation] = toSeq.iterator

}

object ChartPermutations {

  def apply(
    chartTypes: JList[ChartType],
    regions: JList[Region],
    supportedParameters: JMap[String, JList[String]]) = {
    val params = supportedParameters map { case (k, v) => k -> v.toSeq }
    new ChartPermutations(chartTypes.toSeq, regions.toSeq, params.toMap)
  }


}