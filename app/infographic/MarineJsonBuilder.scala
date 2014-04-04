package infographic

import play.api.libs.json._
import charts.Region
import charts.builder.spreadsheet.MarineBuilder
import charts.graphics.BeerCoaster

object MarineJsonBuilder extends ChartJsonBuilder {

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