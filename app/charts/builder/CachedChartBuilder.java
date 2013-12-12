package charts.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import play.Play;
import service.EventManager;
import service.OrderedEvent;
import charts.Chart;
import charts.ChartType;
import charts.Region;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
  public synchronized List<Chart> getCharts(String id, DataSourceFactory dsf,
      ChartType type, List<Region> regions, Map<String, String> parameters)
          throws Exception {
    return filter(chartCache.getCharts(id, dsf) , type, regions, parameters);
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

