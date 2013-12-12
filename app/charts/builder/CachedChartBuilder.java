package charts.builder;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.libs.F;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CachedChartBuilder implements ChartBuilder {

  private final ChartCache chartCache;

  @Inject
  public CachedChartBuilder(ChartCache chartCache) {
    this.chartCache = chartCache;
  }

  @Override
  public List<Chart> getCharts(String id, DataSourceFactory dsf,
      ChartType type, List<Region> regions, Map<String, String> parameters)
          throws Exception {
    final F.Either<Exception, List<Chart>> possiblyCharts =
        chartCache.getCharts(id, dsf);
    if (possiblyCharts.right.isDefined()) {
      final List<Chart> charts = possiblyCharts.right.get();
      return filter(charts, type, regions, parameters);
    } else {
      throw possiblyCharts.left.get();
    }
  }

  private List<Chart> filter(List<Chart> charts, ChartType type,
      List<Region> regions, Map<String, String> parameters) {
    List<Chart> filtered = Lists.newArrayList();
    if(charts != null) {
      for(Chart chart : charts) {
        if(type != null && type != chart.getDescription().getType()) {
          continue;
        }
        if(regions != null && !regions.isEmpty() && !regions.contains(chart.getDescription().getRegion())) {
          continue;
        }
        Map<String, String> cparams = chart.getDescription().getParameters();
        if(parameters != null && cparams != null) {
          for(Map.Entry<String, String> me: parameters.entrySet()) {
            if(cparams.containsKey(me.getKey()) &&
                !StringUtils.equals(me.getValue(), cparams.get(me.getKey()))) {
              continue;
            }
          }
        }
        filtered.add(chart);
      }
    }
    return filtered;
  }
}

