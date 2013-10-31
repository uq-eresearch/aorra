package charts.builder

import com.google.common.base.Preconditions.checkNotNull;
import java.lang.reflect.Modifier
import org.reflections.Reflections
import com.google.inject.Singleton
import java.util.{ Map => JMap, List => JList }

import java.awt.Dimension
import charts.{ Chart, ChartType, Region }
import play.api.Logger
import scala.collection.JavaConversions._

@Singleton
class ChartBuilder {

  type JCharts = JList[Chart]
  type JDataSources = JList[DataSource]
  type JRegions = JList[Region]
  type JParameters = JMap[String, String]
  type JParameterValues = JMap[String, JList[String]]

  val builders: Seq[ChartTypeBuilder] = {
    def isConcrete[T](c: Class[T]) = {
      !c.isInterface && !Modifier.isAbstract(c.getModifiers)
    }
    val builderClasses = new Reflections("charts.builder")
      .getSubTypesOf(classOf[ChartTypeBuilder])
    builderClasses.toSeq flatMap {
      case bc if isConcrete(bc) => {
        Logger.debug("Found chart builder: " + bc.getCanonicalName)
        Some(bc.newInstance())
      }
      case _ => None
    }
  }

  def getCharts(ds: JDataSources, ct: ChartType, r: JRegions, dim: Dimension,
    p: JParameters): JCharts = {
    checkNotNull(p)
    withBuilders(ds.toSeq, ct) { builder =>
      builder.build(ds, ct, r, dim, p)
    }.flatten.sortBy(_.getDescription.getRegion).toVector
  }

  def getCharts(ds: JDataSources, r: JRegions, dim: Dimension): JCharts =
    getCharts(ds, null, r, dim, Map.empty[String, String])

  def getParameters(ds: JDataSources, ct: ChartType): JParameterValues =
    withBuilders(ds.toSeq, ct) { builder =>
      builder.getParameters(ds, ct)
    }.foldLeft(Map.empty[String, JList[String]])(_ ++ _)

  protected def withBuilders[A](datasources: Seq[DataSource], ct: ChartType)(
    f: ChartTypeBuilder => A): Seq[A] =
    builders flatMap {
      case builder if (builder.canHandle(ct, datasources)) => Some(f(builder))
      case _ => None
    }

}
