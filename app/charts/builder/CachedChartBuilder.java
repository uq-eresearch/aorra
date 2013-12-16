package charts.builder;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CachedChartBuilder implements ChartBuilder {

  private final Timeout timeout = new Timeout(Duration.create(60, "seconds"));
  private final ChartCache chartCache;

  @Inject
  public CachedChartBuilder(ChartCache chartCache) {
    this.chartCache = chartCache;
  }

  @Override
  public List<Chart> getCharts(String id,
      ChartType type, List<Region> regions, Map<String, String> parameters)
          throws Exception {
    final Future<List<Chart>> futureCharts = chartCache.getCharts(id);
    // We leverage Await.result() here to throw any exception that the actor
    // may have encountered.
    final List<Chart> charts = Await.result(futureCharts, timeout.duration());
    return filter(charts, type, regions, parameters);
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
        final Map<String, String> cparams = chart.getDescription().getParameters();
        if(parameters != null && cparams != null) {
          if(!Maps.difference(Maps.filterKeys(parameters, new Predicate<String>() {
            @Override
            public boolean apply(@Nullable String key) {
              return cparams.containsKey(key);
            }}), cparams).areEqual()) {
            continue;
          }
        }
        filtered.add(chart);
      }
    }
    return filtered;
  }
}

